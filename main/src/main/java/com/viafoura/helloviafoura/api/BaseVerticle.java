package com.viafoura.helloviafoura.api;

import com.google.inject.Injector;
import com.viafoura.common.vertx.ServiceConnector;
import com.viafoura.common.vertx.ServiceConnectorRegistry;
import com.viafoura.common.vertx.ServiceIdRegistry;
import com.viafoura.common.vertx.handlers.CorsHandlerFactory;
import com.viafoura.common.vertx.handlers.ResponseEndTimeoutHandler;
import com.viafoura.helloviafoura.api.service.TokenSecurityHandler;
import com.viafoura.helloviafoura.config.ServerConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;


abstract class BaseVerticle extends AbstractVerticle {

    private HttpServer httpServer;
    private ServiceConnectorRegistry serviceConnectorRegistry;

    @Override
    public void start(Promise<Void> future) {
        this.getVertx().executeBlocking(this::initalize, (result) -> {
            if (result.failed()) {
                future.fail(result.cause());
                System.exit(1);
            } else {
                future.complete();
            }
        });
    }

    /**
     * order of operations: - wait for ongoing requests to finish in each ServiceConnector (previously
     * AbstractVerticles) before proceeding.  this is to attempt to avoid weird states if the server crashes, etc. -
     * close http server - signal that we can proceed with stopping
     */
    @Override
    public void stop(Future<Void> future) {
        serviceConnectorRegistry.getServiceConnectors().forEach(ServiceConnector::stop);
        httpServer.close();
        future.complete();
    }

    protected abstract Injector getInjector();

    private void initalize(Promise<Void> promise) {

        /* entry point for guice dependency injection */
        Injector injector = getInjector();

        serviceConnectorRegistry = injector.getInstance(ServiceConnectorRegistry.class);

        ServerConfig serverConfig = injector.getInstance(ServerConfig.class);

        /*
         * this will attempt to load the json resource created from the api-generated module
         * once successfully loaded we need to build up the router
         *
         * Options
         * - we require use of the Operation context within the RoutingContext, so we configure this
         *
         * Security
         * - we check routes to see if they use the TokenInCookie security
         * if so, we need to verify the JWT passed in by the calling user
         *
         * Global
         * - we enforce a timeout for all calls
         * - we handle bodies
         * - we handle cookies
         * - we provide CORS support since our primary client is browser-based
         *
         * after the router is built up, we need to connect each operation to the eventbus
         * we have it implemented such (with the generated code) so that each operationId can be
         * passed in as both the operationId and address on the eventbus
         *
         * we then combine the router with some server options to create the underlying httpserver
         */
        OpenAPI3RouterFactory.create(getVertx(), serverConfig.getApiSpecPath(), result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }

            OpenAPI3RouterFactory factory = result.result();

            RouterFactoryOptions routerFactoryOptions = injector.getInstance(RouterFactoryOptions.class);
            factory.setOptions(routerFactoryOptions);
            factory.addSecurityHandler(serverConfig.getTokenInCookieKey(), RoutingContext::next);
            factory.addSecurityHandler(serverConfig.getTokenInCookieKey(),
                    injector.getInstance(TokenSecurityHandler.class));
            factory.addSecurityHandler(serverConfig.getTokenKey(), RoutingContext::next);
            factory.addSecurityHandler(serverConfig.getTokenKey(), injector.getInstance(TokenSecurityHandler.class));

            factory.addGlobalHandler(BodyHandler.create());
            factory.addGlobalHandler(new ResponseEndTimeoutHandler(serverConfig.getServerGlobalTimeoutMs()));
            factory.addGlobalHandler(CorsHandlerFactory.createInstance());

            serviceConnectorRegistry.getServiceConnectors().forEach(ServiceConnector::start);

            ServiceIdRegistry serviceIdRegistry = injector.getInstance(ServiceIdRegistry.class);
            serviceIdRegistry
                    .getServiceIds()
                    .forEach(id -> factory.mountOperationToEventBus(id.toString(), id.toString()));

            Router router = factory.getRouter();
            // Serve static resources found under resources/webroot
            if (serverConfig.getSwaggerUiEnabled()) {
                router.route("/*").handler(StaticHandler.create());
            }
            HttpServerOptions httpServerOptions = injector.getInstance(HttpServerOptions.class);
            httpServer = getVertx().createHttpServer(httpServerOptions);

            httpServer.requestHandler(router).listen(serverStartupResult -> {
                if (serverStartupResult.failed()) {
                    promise.fail(serverStartupResult.cause());
                } else {
                    promise.complete();
                }
            });
        });
    }
}
