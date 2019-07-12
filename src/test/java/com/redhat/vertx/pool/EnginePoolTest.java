package com.redhat.vertx.pool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import com.redhat.vertx.Engine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class EnginePoolTest {

    @Test
    public void testWithFailingResolver() throws Exception {
        EnginePool pool = new EnginePool(new FailingPipelineResolver());
        CompletableFuture<Engine> future = pool.getEngineByPipelineName("foo").toCompletableFuture();
        try {
            future.get();
            fail("Exception must be thrown.");
        } catch (ExecutionException e) {
            assertEquals(RuntimeException.class, e.getCause().getClass());
            assertEquals("You shall not pass.", e.getCause().getMessage());
        }
    }

    public static class FailingPipelineResolver  implements PipelineResolver {
        @Override
        public CompletionStage<String> getExecutablePipelineByName(String pipelineName) {
            return CompletableFuture.failedFuture(new RuntimeException("You shall not pass."));
        }
    }
}
