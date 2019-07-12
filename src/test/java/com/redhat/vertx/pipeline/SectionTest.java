package com.redhat.vertx.pipeline;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SectionTest {

    @Test
    public void assertSectionBasicCreation() {
        Section s = new Section(new JsonObject());
        assertThat(s).isNotNull();

        assertThat(s).hasFieldOrProperty("name");
        assertThat(s).hasFieldOrProperty("steps");
    }

    @Test
    public void assertSectionCreation() {
        JsonObject sectionDef = new JsonObject();
        sectionDef.put("section", "enrich");

        Section s = new Section(sectionDef);
        assertThat(s.name).isEqualTo("enrich");
    }
}
