package com.redhat.vertx.pipeline;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StepTest {

    @Test
    public void assertStepCreation() {
        JsonObject sectionDef = new JsonObject();
        sectionDef.put("section", "enrich");

        Section s = new Section(sectionDef);

        assertThat(s).hasFieldOrProperty("name");
        assertThat(s).hasFieldOrProperty("steps");
        assertThat(s.name).isEqualTo("enrich");
    }
}
