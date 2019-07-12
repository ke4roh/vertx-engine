package com.redhat.vertx.pipeline;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import com.redhat.ResourceUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class PipelineTest {

    @Test
    public void assertPipelineHasSections() {
        String s = Objects.requireNonNull(ResourceUtils.fileContentsFromResource("hello-world-pipeline.json"));
        Pipeline p = new Pipeline(s);
        assertThat(p).hasFieldOrProperty("sections");
        assertThat(p).hasNoNullFieldsOrProperties();
    }

    @Test
    public void assertSectionCreationOnPipeline() {
        String s = Objects.requireNonNull(ResourceUtils.fileContentsFromResource("hello-world-pipeline.json"));
        Pipeline p = new Pipeline(s);

        assertThat(p.sections).isNotEmpty();
    }

    @Test
    public void assertBasicHelloWorld() throws ExecutionException, InterruptedException {
        // Setup conditions for our "storage"
        // TODO less hacky for checking the log/event/record
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        String s = Objects.requireNonNull(ResourceUtils.fileContentsFromResource("hello-world-pipeline.json"));
        Pipeline p = new Pipeline(s);

        JsonObject doc = new JsonObject();
        doc.put("name","Jason");
        CompletionStage<String> returned = p.execute(new ExecutionData(doc.toString()));

        doc.put("greetings", "Jason");

        assertEquals(doc, new JsonObject(returned.toCompletableFuture().get()));

        // TODO - this is the loopy assert vvv
        assertThat(outContent.toString().trim()).isEqualTo("A Step completed with the result: hello, Jason");

        System.setOut(originalOut);
    }
}
