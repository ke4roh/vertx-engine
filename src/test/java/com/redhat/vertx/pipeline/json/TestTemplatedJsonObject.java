package com.redhat.vertx.pipeline.json;

import com.redhat.ResourceUtils;
import com.redhat.vertx.pipeline.templates.JinjaTemplateProcessor;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class TestTemplatedJsonObject {
    @Test
    public void testGetMap() {
        JsonObject jo = new JsonObject();
        jo.put("foo","bar");

        JsonObject jo2 = new TemplatedJsonObject(jo,new JinjaTemplateProcessor());
        Map<String,Object> m = jo2.getMap();
        assertEquals(1,m.size());
        assertEquals("bar",m.get("foo"));
        Iterator mi = m.entrySet().iterator();
        assertTrue(mi.hasNext());
        Map.Entry<String,Object> entry = (Map.Entry<String, Object>) mi.next();
        assertEquals("foo",entry.getKey());
        assertEquals("bar", entry.getValue());
        assertFalse(mi.hasNext());
        try {
            mi.next();
            fail();
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    public void testGetDeepArray() {
        JsonObject jo = new JsonObject("{\"doc\":{\"x\":\"foo\",\"words\":[\"The\",\"quick\",\"brown\"]},\"q\":\"{{ doc.words[1] }}\"}");
        TemplatedJsonObject tjo = new TemplatedJsonObject(jo,new JinjaTemplateProcessor());
        assertEquals("quick",tjo.getValue("q"));
    }

    @Test
    public void testSplit() {
        JsonObject jo = new JsonObject("{\"doc\":{\"x\":\"foo\",\"intro\":\"The quick brown fox\"},\"q\":\"{{ doc.intro|split|tojson }}\"}");
        TemplatedJsonObject tjo = new TemplatedJsonObject(jo,new JinjaTemplateProcessor());
        assertEquals("[\"The\",\"quick\",\"brown\",\"fox\"]",tjo.getValue("q"));
    }

    @Test
    public void testVarSubstitution() {
        JsonObject doc = new JsonObject(
                ResourceUtils.fileContentsFromResource(
                        "com/redhat/vertx/pipeline/step/varSubstitutionTestDoc.json"
                ));
        JsonObject vars = new JsonObject("{\"from\":\"{{ doc.intro|split|tojson }}\", \"register\":\"cash\", \"q\":\"{{ doc.intro|split|tojson }}\"}");
        JsonObject env = vars.copy();
        env.put("doc",doc);
        TemplatedJsonObject tjo = new TemplatedJsonObject(env,new JinjaTemplateProcessor(),"doc");
        assertNotNull(tjo.toString());
        assertEquals(JsonObjectMapView.class,tjo.getMap().get("doc").getClass());
        assertEquals("[\"This\",\"should\",\"not\",\"{{var}}\",\"be\",\"substituted\"]",tjo.getValue("q"));
    }
}
