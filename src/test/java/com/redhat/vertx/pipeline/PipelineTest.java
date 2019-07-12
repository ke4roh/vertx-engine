package com.redhat.vertx.pipeline;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import com.redhat.ResourceUtils;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        String s = Objects.requireNonNull(ResourceUtils.fileContentsFromResource("hello-world-pipeline.json"));
        Pipeline p = new Pipeline(s);

        CompletionStage<String> returned = p.execute(new ExecutionData("Jason"));

        assertThat(outContent.toString().trim()).isEqualTo("A Step completed with the result: hello, Jason");

        System.setOut(originalOut);
    }
}
