package com.viafoura.helloviafoura.api.service;

import com.viafoura.common.service.auth.PermissionLevel;
import com.viafoura.common.service.auth.SectionJwtAuthenticator;
import com.viafoura.helloviafoura.config.ServerConfig;
import io.swagger.v3.oas.models.Operation;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({@Inject}))
public final class TokenSecurityHandler implements Handler<RoutingContext> {

    /* the order of these is intentional, from highest level of permission to lowest */
    private static final List<PermissionLevel> PERMISSION_LEVELS = List.of(
            PermissionLevel.CLIENT,
            PermissionLevel.ADMIN,
            PermissionLevel.MOD,
            PermissionLevel.USER,
            PermissionLevel.CONTAINER,
            PermissionLevel.OPTIONAL
    );

    private final ServerConfig serverConfig;
    private final SectionJwtAuthenticator sectionJwtAuthenticator;

    /**
     * the operation model key is how vert.x OpenApi3RouterFactory passes the operation information to the context this
     * is configurable so see below:
     * <p>
     * this security handler grabs the TokenInCookie arguments the calling class is expected to use this handler ON the
     * TokenInCookie security, so it must be there; it may not have values however - see usernotification.yaml if this
     * is the case
     * <p>
     * we then descend through permission level (client -> optional) to see lowest permission this route requires and
     * then check the user JWT against this, and the section
     */
    @Override
    public void handle(RoutingContext routingContext) {
        Operation operation = routingContext.get(serverConfig.getOperationModelKey());

        Set<PermissionLevel> permissionLevels = operation
                .getSecurity()
                .stream()
                .filter(sr -> sr.containsKey(serverConfig.getTokenInCookieKey()))
                .map(sr -> sr.get(serverConfig.getTokenInCookieKey()))
                .findFirst()
                .orElseThrow()
                .stream()
                .map(PermissionLevel::fromString)
                .collect(Collectors.toSet());

        PermissionLevel permissionLevel = PERMISSION_LEVELS
                .stream()
                .filter(permissionLevels::contains)
                .findFirst()
                .orElse(PermissionLevel.OPTIONAL);

        sectionJwtAuthenticator.authenticate(routingContext, permissionLevel);
    }
}
