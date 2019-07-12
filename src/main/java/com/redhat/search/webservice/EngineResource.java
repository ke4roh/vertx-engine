package com.redhat.search.webservice;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.redhat.vertx.pool.EnginePool;
import com.redhat.vertx.pool.PipelineResolver;

/**
 * This class is responsible for managing the getEngineByPipelineName instance, which pipeline it runs, and translation of a
 * pipeline name into the full verbose pipeline.
 *
 * This implementation is specific to search queries.
 */
@Path("/query")
public class EngineResource {
    private EnginePool enginePool;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{pipeline}")
    public CompletionStage<String> greeting(@PathParam("pipeline") String pipeline, String body) {
        return getEnginePool().getEngineByPipelineName(pipeline)
                .thenComposeAsync(
                        engine -> engine.execute(pipeline)
                                  .thenApplyAsync(s -> s + body)
                );
    }

    private EnginePool getEnginePool() {
        if (enginePool==null) {
            enginePool = new EnginePool(new PipelineResolver() {
                @Override
                public CompletionStage<String> getExecutablePipelineByName(String pipelineName) {
                    return CompletableFuture.completedFuture(pipelineName);
                }
            });
        }
        return enginePool;
    }
}
