package com.viafoura.helloviafoura.server.api.generated;

import org.openapitools.server.api.MainApiException;

import com.viafoura.common.vertx.ApiResponse;
import com.viafoura.common.vertx.context.RequestContext;

import io.reactivex.Single;

import java.util.*;

public interface HelloViafouraHealthApi  {
    Single<ApiResponse<Void>> healthy();

}
