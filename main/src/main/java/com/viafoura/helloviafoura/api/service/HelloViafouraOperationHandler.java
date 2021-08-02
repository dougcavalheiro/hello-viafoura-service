package com.viafoura.helloviafoura.api.service;

import com.viafoura.common.vertx.ApiResponse;
import com.viafoura.common.vertx.ApiResponseFactory;
import com.viafoura.helloviafoura.server.api.generated.HelloViafouraOperationApi;
import com.viafoura.helloviafoura.server.model.generated.HelloMessageOutput;
import io.reactivex.Single;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({@Inject}))
public class HelloViafouraOperationHandler implements HelloViafouraOperationApi {

    @Override
    public Single<ApiResponse<HelloMessageOutput>> getMyHello(String name) {
        String message = String.format("Hello %s. Welcome to Viafoura.", name);
        HelloMessageOutput output = new HelloMessageOutput(message);
        return Single.just(output)
                .map(ApiResponseFactory::accepted)
                .onErrorResumeNext(ServiceErrorHandler::handleError);
    }
}
