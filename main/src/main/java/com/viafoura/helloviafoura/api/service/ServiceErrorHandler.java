package com.viafoura.helloviafoura.api.service;

import com.viafoura.common.vertx.ApiExceptionFactory;
import io.reactivex.Single;
import java.util.NoSuchElementException;

public final class ServiceErrorHandler {

    private ServiceErrorHandler() {
    }

    public static <T> Single<T> handleError(Throwable error) {
        if (error instanceof IllegalStateException) {
            return Single.error(ApiExceptionFactory.badRequest());
        }
        if (error instanceof NoSuchElementException) {
            return Single.error(ApiExceptionFactory.notFound());
        }

        return Single.error(error);
    }
}
