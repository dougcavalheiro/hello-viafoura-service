package com.viafoura.helloviafoura.api;

import com.codahale.metrics.SharedMetricRegistries;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.viafoura.common.metrics.client.ViafouraMetricsClient;
import com.viafoura.common.metrics.client.ViafouraMetricsClientFactory;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MainVerticle extends BaseVerticle {

    private static final String SERVICE_CONFIG_NAME = "helloviafoura";

    public MainVerticle() {
        // It's imperative to do this as soon as possible as many classes use the SharedMetricRegistries singleton
        log.info("Initializing shared metrics registry.");
        ViafouraMetricsClient viafouraMetricsClient = ViafouraMetricsClientFactory.createInstance();
        SharedMetricRegistries.setDefault(SERVICE_CONFIG_NAME, viafouraMetricsClient);
        log.info("Initialized & registered shared metrics registry.");
    }

    /**
     * /** This main entry could be used to run the service locally. Check section '### Local Run Within The IDE' of the
     * README.md file for instructions on how to use this.
     *
     * @param args
     */
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        Vertx.vertx().deployVerticle(MainVerticle.class.getName());
    }

    @Override
    protected Injector getInjector() {
        return Guice.createInjector(Stage.PRODUCTION, new ApiServiceModule(getVertx(), config()));
    }
}
