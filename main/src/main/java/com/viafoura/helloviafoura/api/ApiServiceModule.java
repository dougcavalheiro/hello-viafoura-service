package com.viafoura.helloviafoura.api;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.viafoura.common.service.auth.SectionJwtAuthenticator;
import com.viafoura.common.service.vertx.SectionedAccessTokenExtractor;
import com.viafoura.common.vertx.VerticleAdapter;
import com.viafoura.helloviafoura.api.service.ServiceModule;
import com.viafoura.helloviafoura.config.ConfigModule;
import com.viafoura.helloviafoura.config.ServerConfig;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.SecretOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import java.util.Objects;
import javax.inject.Singleton;


@SuppressWarnings("deprecation")
public final class ApiServiceModule extends AbstractModule {

    private final Vertx vertx;
    private final JsonObject config;

    ApiServiceModule(Vertx vertx, JsonObject config) {
        this.vertx = Objects.requireNonNull(vertx);
        this.config = Objects.requireNonNull(config);
    }

    @Override
    protected void configure() {
        super.configure();

        install(new ConfigModule());
        install(new ServiceModule());

        bind(Vertx.class).toInstance(vertx);
    }

    @Provides
    @Singleton
    VerticleAdapter provideVerticleAdapter() {
        return new VerticleAdapter(vertx, config);
    }

    @Provides
    @Singleton
    RouterFactoryOptions provideRouterFactoryOptions(ServerConfig serverConfig) {
        return new RouterFactoryOptions()
                .setOperationModelKey(serverConfig.getOperationModelKey());
    }

    @Provides
    @Singleton
    HttpServerOptions provideHttpServerOptions(Vertx vertx, ServerConfig serverConfig) {
        HttpServerOptions options = new HttpServerOptions()
                .setPort(serverConfig.getServerPort())
                .setCompressionSupported(true)
                .setDecompressionSupported(true);

        if (vertx.isNativeTransportEnabled()) {
            options
                    .setTcpFastOpen(true)
                    .setTcpQuickAck(true)
                    .setReusePort(true);
        }

        return options;
    }

    @Provides
    @Singleton
    JWTAuthOptions provideJWTAuthOptions(ServerConfig serverConfig) {
        return new JWTAuthOptions()
                .addSecret(new SecretOptions().setSecret(serverConfig.getJwtPrimarySecret()))
                .addSecret(new SecretOptions().setSecret(serverConfig.getJwtSecondarySecret()));
    }

    @Provides
    @Singleton
    SectionJwtAuthenticator provideSectionJwtAuthenticator(Vertx vertx, JWTAuthOptions jwtAuthOptions) {
        return new SectionJwtAuthenticator(vertx, jwtAuthOptions, new SectionedAccessTokenExtractor());
    }
}
