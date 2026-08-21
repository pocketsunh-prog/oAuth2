package com.oauth.server.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

/**
 * Authentication token representing a bearer token (e.g., "tk_...").
 */
public class BearerTokenAuthenticationToken extends AbstractAuthenticationToken {

    private final String token;

    /**
     * Create an unauthenticated bearer token.
     */
    public BearerTokenAuthenticationToken(String token) {
        super(Collections.emptyList());
        this.token = token;
        setAuthenticated(false);
    }

    /**
     * Create an authenticated bearer token with principal.
     */
    public BearerTokenAuthenticationToken(String token, Object principal) {
        super(Collections.emptyList());
        this.token = token;
        super.setAuthenticated(true);
        // Store principal in details
        setDetails(principal);
    }

    /**
     * The bearer token value.
     */
    @Override
    public Object getCredentials() {
        return token;
    }

    /**
     * The authenticated principal (username).
     */
    @Override
    public Object getPrincipal() {
        return getDetails();
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException(
                    "Cannot set this token to trusted - use constructor instead");
        }
        super.setAuthenticated(false);
    }
}
