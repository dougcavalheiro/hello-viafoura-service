package com.viafoura.helloviafoura.server.api.generated;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.viafoura.common.vertx.ServiceConnector;
import com.viafoura.common.vertx.ServiceId;
import com.viafoura.common.vertx.VerticleAdapter;
import com.viafoura.common.vertx.ApiResponse;
import com.viafoura.common.vertx.ApiException;

import com.viafoura.common.vertx.context.RequestContext;
import com.viafoura.common.vertx.context.extractor.RequestContextExtractor;

import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.*;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.MultiMap;

import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.reactivex.Completable;

import org.openapitools.server.api.MainApiException;

@Slf4j
public final class HelloViafouraHealthApiVerticle implements ServiceConnector {

    public static final ServiceId HEALTHY_SERVICE_ID = new ServiceId("healthy");
    public static final Set<ServiceId> ALL_SERVICE_IDS = collectServiceIds();

    // It is imperative that the shared metric registry is set before any verticles are constructed. Otherwise, the following line will throw exception.
    private static final MetricRegistry metricRegistry = SharedMetricRegistries.getDefault();
    private static final int INFLIGHT_REQUEST_SHUTDOWN_RETRIES = 10;
    private static final int INFLIGHT_REQUEST_SHUTDOWN_TIMEOUT = 1000;
    private static final String HEALTHY_SERVICE_ID_METER = "HelloViafouraHealthApi." + capitalize("healthy");
    private static final String HEALTHY_SERVICE_ID_TIMER = HEALTHY_SERVICE_ID_METER + "Timer";
    
    private final AtomicInteger inflightRequests = new AtomicInteger(0);
    private final Vertx vertx;
    private final JsonObject config;
    private final RequestContextExtractor extractor;
    private final HelloViafouraHealthApi service;

    @Inject
    public HelloViafouraHealthApiVerticle(VerticleAdapter adapter, RequestContextExtractor extractor, HelloViafouraHealthApi service) {
        this.vertx = adapter.getVertx();
        this.config = adapter.getConfig();
        this.extractor = extractor;
        this.service = service;
    }

    @Override
    public void start() {
        
        // Consumer for healthy
        vertx.eventBus().<JsonObject> consumer(HEALTHY_SERVICE_ID.toString()).handler(message -> {
            String meterName =  HEALTHY_SERVICE_ID_METER;
            metricRegistry.meter(meterName).mark();
            Timer.Context timer = metricRegistry.timer(HEALTHY_SERVICE_ID_TIMER).time();
            try {
                inflightRequests.incrementAndGet();
                JsonObject context = message.body().getJsonObject("context");


                this.service.healthy().subscribe(
                    result -> {
                        ApiResponse<?> encodedResponse = new ApiResponse<>(
                            result.getStatusCode(),
                            result.getStatusMessage(),
                            Objects.isNull(result.getPayload()) ? null :
                                Base64.getEncoder().encodeToString(Json.encode(result.getPayload()).getBytes())
                        );
                        message.reply(new JsonObject(Json.encode(encodedResponse)));
                        inflightRequests.decrementAndGet();
                    },
                    error -> {
                        manageError(message, error, meterName);
                        timer.stop();
                    }
                );

            } catch (Exception e) {
                manageError(message, e, meterName);
                timer.stop();
            }
        });
        
    }

    @Override
    public void stop() {
      Integer shutdownTimeout = config.getInteger("verticle_shutdown_timeout", INFLIGHT_REQUEST_SHUTDOWN_TIMEOUT);
      Integer shutdownRetries = config.getInteger("verticle_shutdown_retries", INFLIGHT_REQUEST_SHUTDOWN_RETRIES);

       Completable.fromRunnable(() -> {
           if (inflightRequests.get() > 0) {
                log.info("Can't shutdown, requests are still being processed...");
                throw new RuntimeException("Things are still running...");
            }
       })
        .retryWhen(flowable -> flowable.take(shutdownRetries).delay(shutdownTimeout, TimeUnit.MILLISECONDS))
        .doOnComplete(() -> log.debug("No pending requests, shutting down verticle."))
        .subscribe(() -> log.info("Verticle shutdown complete."));
    }


    /**
     * converts thrown exceptions into json error responses. Ideally, we'd fail the vertx message, and add a
     * failureHandler to the router, but the SwaggerRouter handles all failed messages, and provides no way to add
     * a body.
     */
    private void manageError(Message<JsonObject> message, Throwable cause, String meterName) {
        ApiException apiException;
        if (cause instanceof ApiException) {
            apiException = (ApiException)cause;
            log.error("Error was handled in the application.", apiException);
        } else if (cause instanceof IllegalArgumentException || cause instanceof JsonMappingException) {
            apiException = new ApiException(400, "Bad Request", null);
            log.error("Error parsing request parameters or body.", apiException);
        } else {
            apiException = new ApiException(500, "Internal Server Error", null);
            log.error("Unexpected error in " + meterName, apiException);
        }

        ApiException encodedException = new ApiException(
            apiException.getStatusCode(),
            apiException.getStatusMessage(),
            Objects.isNull(apiException.getPayload()) ? null : Base64
                .getEncoder()
                .encodeToString(Json.encode(apiException.getPayload()).getBytes())
        );

        metricRegistry.meter(meterName + ".Error." + apiException.getStatusCode()).mark();
        message.reply(new JsonObject(Json.encode(encodedException)));
        inflightRequests.decrementAndGet();
    }

    private static Set<ServiceId> collectServiceIds() {
        Set<ServiceId> serviceIds = new HashSet<>();
        serviceIds.add(HEALTHY_SERVICE_ID);
        return serviceIds;
    }

    // Converting the string to UpperCamelCase / Pascal case by capitalizing the first letter for metric names
    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
