package com.redhat.vertx.pipeline.json;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import com.redhat.ResourceUtils;
import com.redhat.vertx.pipeline.templates.JinjaTemplateProcessor;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestTemplatedJsonObject {

    @Test
    public void testJsonGet() {
        var obj = new JsonObject();

        obj.put("one", 1).put("two", "two").put("dec", 1.00).put("long", 1L)
           .put("bool", true);

        var tjo = new TemplatedJsonObject(obj, new JinjaTemplateProcessor());
        var tjo2 = new TemplatedJsonObject(obj, new JinjaTemplateProcessor());

        assertThat(tjo.getInteger("one")).isEqualTo(1);
        assertThat(tjo.getInteger("1", 100)).isEqualTo(100);

        assertThat(tjo.getString("two")).isEqualTo("two");
        assertThat(tjo.getValue("2", 2)).isEqualTo(2);

        assertThat(tjo.getFloat("dec")).isBetween(1.0f, 1.1f);
        assertThat(tjo.getFloat("dec-null", 1.5f)).isEqualTo(1.5f);

        assertThat(tjo.getDouble("dec")).isBetween(1.0, 1.1);
        assertThat(tjo.getDouble("dec-null", 1.5)).isEqualTo(1.5);

        assertThat(tjo.getLong("long")).isEqualTo(1L);
        assertThat(tjo.getLong("long-null", 2L)).isEqualTo(2L);

        assertThat(tjo.getBoolean("bool")).isTrue();
        assertThat(tjo.getBoolean("bool-null", false)).isFalse();

        assertThat(tjo.equals(tjo2)).isTrue();
        assertThat(tjo.hashCode()).isNotZero();

        assertThat(tjo.fieldNames()).isEqualTo(obj.fieldNames());

        assertThat(tjo.size()).isEqualTo(obj.size());
    }

    @Test
    public void testUnmodifiable() {
        var tjo = new TemplatedJsonObject(new JsonObject(), new JinjaTemplateProcessor());

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", ""));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", 1));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", 1L));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", 1f));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", 1.0));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", LocalDateTime.now()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", Instant.now()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.putNull("hi"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", true));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", new JsonObject()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", new JsonArray()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", new byte[0]));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", TimeUnit.SECONDS));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", new Object()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.put("hi", new StringBuffer()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.remove("hi"));

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.mergeIn(new JsonObject()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.mergeIn(new JsonObject(), true));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.mergeIn(new JsonObject(), 1));


        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> tjo.readFromBuffer(1, Buffer.buffer()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(tjo::clear);
    }

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
