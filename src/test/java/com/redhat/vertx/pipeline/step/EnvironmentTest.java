package com.redhat.vertx.pipeline.step;


import com.redhat.vertx.pipeline.Environment;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class EnvironmentTest {
    @Test
    public void testOverlay() {
        JsonObject doc = new JsonObject();
        doc.put("foo","bar");
        doc.put("bat","bazz");
        doc.put("george",1);
        JsonObject env = new JsonObject();
        env.put("bat","house");
        env.put("lemon","tree");

        Environment e = new Environment(doc,env);
        assertEquals("bar",e.getString("foo"));
        assertEquals("house", e.getString("bat"));
        assertEquals("tree", e.getString("lemon"));
        assertNull(e.getString("york"));
        assertEquals("bon", e.getString("york","bon"));

        assertEquals(1,e.getInteger("george"));
        assertNull(e.getInteger("zero"));
    }

    @Test
    public void testNumbers() {
        JsonObject doc = new JsonObject();
        doc.put("john",1);
        doc.put("paul","two");
        JsonObject env = new JsonObject();
        env.put("ringo",4d);
        Environment e = new Environment(doc,env);

        assertEquals(4d,e.getValue("ringo"));
        assertEquals(1,e.getValue("john"));
        assertEquals(Integer.class,e.getValue("john").getClass());
        assertEquals(1d,e.getDouble("john"));

        assertEquals(42, e.getValue("life",42));

        try {
            e.getInteger("paul");
            fail("Expected ClassCastException");
        } catch (ClassCastException ex) {
            // exptected
        }
    }

    @Test
    public void testMapConversion() {
        JsonObject jo = new JsonObject().put("a","x");
        jo.put("jo",new JsonObject().put("b","y"));
        JsonArray ja = new JsonArray();
        ja.add(1);
        ja.add("two");
        jo.put("ja",ja);

        JsonObject env = new JsonObject();
        env.put("foo","bar");

        Map<String, Object> eMap = new Environment(jo, env).getMap();

        assertEquals("bar",eMap.get("foo"));
        assertEquals("x",eMap.get("a"));
        assertEquals(1,((List<Object>)eMap.get("ja")).get(0));
        assertEquals("two",((List<Object>)eMap.get("ja")).get(1));
        assertEquals("y",((Map)eMap.get("jo")).get("b"));

        assertEquals(Arrays.asList("a","jo","ja","foo"),new ArrayList(eMap.keySet()));
        assertEquals(4,eMap.size());
    }

}
