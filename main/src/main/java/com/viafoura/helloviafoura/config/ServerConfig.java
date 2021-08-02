package com.viafoura.helloviafoura.config;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.Getter;

@Singleton
@Getter
public class ServerConfig {

    private final String apiSpecPath = "openapi.json";
    private final String jwtPrimarySecret;
    private final String jwtSecondarySecret;
    private final String operationModelKey = "operationPOJO";
    private final String tokenInCookieKey = "TokenInCookie";
    private final String tokenKey = "Token";
    private final Long serverGlobalTimeoutMs;
    private final Integer serverPort;
    private final Boolean swaggerUiEnabled;

    @Inject
    public ServerConfig(
            @Named("service.jwt.primary.secret") String jwtPrimarySecret,
            @Named("service.jwt.secondary.secret") String jwtSecondarySecret,
            @Named("service.server.global.timeout.ms") Long serverGlobalTimeoutMs,
            @Named("service.server.port") Integer serverPort,
            @Named("service.swagger_ui.enabled") Boolean swaggerUiEnabled) {
        this.jwtPrimarySecret = jwtPrimarySecret;
        this.jwtSecondarySecret = jwtSecondarySecret;
        this.serverGlobalTimeoutMs = serverGlobalTimeoutMs;
        this.serverPort = serverPort;
        this.swaggerUiEnabled = swaggerUiEnabled;
    }
}
