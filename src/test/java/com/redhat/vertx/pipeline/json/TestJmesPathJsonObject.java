package com.redhat.vertx.pipeline.json;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJmesPathJsonObject {
    @Test
    public void testGetMap() {
        JsonObject jo = new JsonObject("{\"doc\":{\"x\":\"foo\"}}");
        JmesPathJsonObject jmes = new JmesPathJsonObject(jo);
        Map m = jmes.getMap();
        assertEquals(1,m.size());
        assertEquals(jo.getJsonObject("doc").getMap(),m.get("doc"));
        assertEquals("foo",m.get("doc.x"));
        assertEquals("{doc={x=foo}}", m.toString());
    }
}
