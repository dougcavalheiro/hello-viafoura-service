package com.viafoura.helloviafoura.api.service;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.viafoura.common.vertx.ServiceConnector;
import com.viafoura.common.vertx.ServiceId;
import com.viafoura.common.vertx.context.extractor.HeadersContextExtractor;
import com.viafoura.common.vertx.context.extractor.UserContextExtractor;
import com.viafoura.helloviafoura.server.api.generated.HelloViafouraOperationApi;
import com.viafoura.helloviafoura.server.api.generated.HelloViafouraOperationApiVerticle;
import java.util.Set;
import java.util.stream.Stream;

public final class ServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        super.configure();

        bind(HelloViafouraOperationApi.class).to(HelloViafouraOperationHandler.class);

        // connect all ServiceConnectors
        // this will start any auxiliary dependencies of each ServiceConnector and start the service itself
        // we further need to mount all implemented operations (ServiceIds) below
        Multibinder<ServiceConnector> serviceConnectorMultibinder = Multibinder
                .newSetBinder(binder(), ServiceConnector.class);
        Stream.of(
                // generated verticles
                HelloViafouraOperationApiVerticle.class
        ).forEach(clazz -> serviceConnectorMultibinder.addBinding().to(clazz));

        Multibinder<ServiceId> serviceIdMultibinder = Multibinder.newSetBinder(binder(), ServiceId.class);
        Stream.of(
                HelloViafouraOperationApiVerticle.ALL_SERVICE_IDS
        )
                .flatMap(Set::stream)
                .forEach(serviceId -> serviceIdMultibinder.addBinding().toInstance(serviceId));

        // register visitors of any given request's headers
        // this is used to create the RequestContext, if there is authentication, which is then passed into the API
        Multibinder<HeadersContextExtractor> extractorMultibinder = Multibinder
                .newSetBinder(binder(), HeadersContextExtractor.class);
        extractorMultibinder.addBinding().to(UserContextExtractor.class);
    }
}
