package com.guicedee.rest.test;

import com.google.inject.Inject;
import com.guicedee.client.scopes.CallScoper;
import com.guicedee.client.scopes.CallScopeProperties;
import com.guicedee.client.scopes.CallScopeSource;
import jakarta.ws.rs.*;

/**
 * Test resource that verifies the call scope is properly entered
 * when a REST endpoint is invoked.
 */
@ApplicationPath("rest")
@Path("callscope")
@Produces("application/json")
public class CallScopeResource {

    @Inject
    private CallScoper callScoper;

    @Inject
    private CallScopeProperties callScopeProperties;

    /**
     * Returns whether the call scope is active and the source is set to Rest.
     */
    @GET
    @Path("check")
    public String checkCallScope() {
        boolean scopeActive = callScoper.isStartedScope();
        CallScopeSource source = callScopeProperties.getSource();
        return "scopeActive=" + scopeActive + ",source=" + source;
    }

    /**
     * Returns the call scope source.
     */
    @GET
    @Path("source")
    public String getSource() {
        return callScopeProperties.getSource().name();
    }
}

