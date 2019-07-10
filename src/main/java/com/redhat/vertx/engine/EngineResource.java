package com.redhat.vertx.engine;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;

/**
 * This class is responsible for managing the engine instance, which pipeline it runs, and translation of a
 * pipeline name into the full verbose pipeline.
 *
 * This implementation is specific to search queries.
 */
@Path("/query")
public class EngineResource {

    @Inject
    EnginePool engines;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{pipeline}")
    public CompletionStage<String> greeting(@PathParam("pipeline") final String pipeline, final String body) {
        return engines.engine(pipeline).thenCompose((engine, throwable) -> {
            engine.execute(body);
        });
    }

}