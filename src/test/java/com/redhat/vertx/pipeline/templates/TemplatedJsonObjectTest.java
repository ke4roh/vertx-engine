package com.redhat.vertx.pipeline.templates;

import com.redhat.vertx.pipeline.json.TemplatedJsonObject;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplatedJsonObjectTest {
    @Test
    public void testTemplatedJsonObject() {
        JsonObject doc = new JsonObject();
        doc.put("boat","{{untouched}}");

        JsonObject data = new JsonObject();
        data.put("doc",doc);
        data.put("q","{{there}}");
        data.put("there","{{doc.boat}}");
        TemplatedJsonObject tjo = new TemplatedJsonObject(data, new JinjaTemplateProcessor(), "doc");
        assertThat(tjo.getValue("q")).isEqualTo("{{untouched}}");
    }
}
