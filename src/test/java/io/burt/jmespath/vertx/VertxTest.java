package io.burt.jmespath.vertx;

import io.burt.jmespath.JmesPathRuntimeTest;
import io.burt.jmespath.JmesPathType;
import io.burt.jmespath.RuntimeConfiguration;
import io.burt.jmespath.Adapter;
import org.assertj.core.api.AbstractIntegerAssert;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

import static io.burt.jmespath.JmesPathType.*;
import static org.assertj.core.api.Assertions.assertThat;

public class VertxTest extends JmesPathRuntimeTest<Object> {
  @Override
  protected Adapter<Object> createRuntime(RuntimeConfiguration configuration) { return new VertxRuntime(configuration); }

    @Test
    public void testLists() throws NoSuchAlgorithmException {
        byte[] bytes = new byte[20];
        SecureRandom.getInstanceStrong().nextBytes(bytes);
        String bytesString = Base64.getEncoder().encodeToString(bytes);

        var map = new HashMap<String, Object>();
        map.put("text","hi");

        var backingArr = Arrays.asList(
                    1L,                                        // 0
                    1f,                                        // 1
                    1,                                         // 2
                    1.2,                                       // 3
                    bytesString,                               // 4
                    "",                                        // 5
                    true,                                      // 6
                    Arrays.asList(new Object[]{"hi"}),         // 7
                    map,                                       // 8
                    null,                                      // 9
                    false                                      // 10
        );

        var runtime = createRuntime(RuntimeConfiguration.defaultConfiguration());
        var converted = runtime.createArray(backingArr);
        var newList = runtime.toList(converted);

        assertThat(newList).isNotEqualTo(backingArr);
        assertThat(newList.get(0)).isEqualTo(1L);
        assertThat(newList.get(1)).isEqualTo(1f);
        assertThat(newList.get(2)).isEqualTo(1);
        assertThat(newList.get(3)).isEqualTo(1.2);
        assertThat(newList.get(4)).isEqualTo(backingArr.get(4));
        assertThat(newList.get(5)).isEqualTo("");
        assertThat(newList.get(6)).isEqualTo(true);
        assertThat(runtime.toList(newList.get(7))).isEqualTo(backingArr.get(7));
        assertThat(parseObject(runtime,(newList.get(8)))).isEqualTo(backingArr.get(8));
        assertThat(newList.get(9)).isNull();
        assertThat(newList.get(10)).isEqualTo(Boolean.FALSE);

        var backingIt = backingArr.iterator();
        var newIt = newList.iterator();
        for (var i=0;i<newList.size();i++) {
            var expected = backingIt.next();
            var actual = fromAdapted(runtime,newIt.next());
            assertThat(fromAdapted(runtime, actual)).isEqualTo(expected);
        }
    }

    @Test
    public void testTruthy() {
        var list = Arrays.asList(false);
        var map = new HashMap<String, Object>();
        map.put("text","hi");

        var rt = createRuntime(RuntimeConfiguration.defaultConfiguration());
        Arrays.asList(true,1,0l,1.1d,"yellow","false","null",map,list).stream().map(x -> toAdapted(rt,x))
                .iterator().forEachRemaining(x-> assertThat(rt.isTruthy(x)).isTrue());
        Arrays.asList(false,"",null,Collections.emptyList(),Collections.emptyMap()).stream().map(x -> toAdapted(rt,x))
                .iterator().forEachRemaining(x-> assertThat(rt.isTruthy(x)).isFalse());

        try {
            rt.isTruthy(this);
            assertThat(false).isTrue();
        } catch (IllegalStateException e) {
            // expected
        }

    }

    @Test
    public void testTypeOf() {
        var rt = createRuntime(RuntimeConfiguration.defaultConfiguration());
        checkTypeOf(rt, 1,NUMBER);
        checkTypeOf(rt, 1l,NUMBER);
        checkTypeOf(rt, 1.1d,NUMBER);
        checkTypeOf(rt, 1.1f,NUMBER);
        checkTypeOf(rt, false,BOOLEAN);
        checkTypeOf(rt, true,BOOLEAN);
        checkTypeOf(rt, "lorem",STRING);
        checkTypeOf(rt, 1,NUMBER);
        checkTypeOf(rt, Arrays.asList(1,2,3),ARRAY);
        checkTypeOf(rt, Collections.emptyMap(), OBJECT);

        try {
            rt.typeOf(this);
            assertThat(false).isTrue();
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testCompare() {
        var rt = createRuntime(RuntimeConfiguration.defaultConfiguration());
        compare(rt, "foo", "foo").isEqualTo(0);
        compare(rt, "a", "b").isEqualTo("a".compareTo("b"));
        compare(rt, "b", "a").isEqualTo("b".compareTo("a"));

        compare(rt, 1, 1).isEqualTo(0);
        compare(rt, 1, 2).isEqualTo(Integer.valueOf(1).compareTo(2));
        compare(rt, 2, 1).isEqualTo(Integer.valueOf(2).compareTo(1));

        compare(rt, 1.1d, 1.1d).isEqualTo(0);
        compare(rt, 1.1d, 2.1d).isEqualTo(Double.valueOf(1.1d).compareTo(2.1d));
        compare(rt, 2.1d, 1.1d).isEqualTo(Double.valueOf(2.1d).compareTo(1.1d));

        compare(rt, 1l, 2.1d).isEqualTo(Double.valueOf(1l).compareTo(2.1d));

        compare(rt, null, null).isEqualTo(0);

        compare(rt, true, true).isEqualTo(0);
        compare(rt, false, true).isEqualTo(-1);
        compare(rt, true, false).isEqualTo(-1);

        var array1 = Arrays.asList("a","b","c");
        var array2 = Arrays.asList("do","re","mi");
        compare(rt, array1, array1).isEqualTo(0);
        compare(rt, array1, array2).isEqualTo(-1);
        compare(rt, Collections.emptyList(), Collections.emptyList()).isEqualTo(0);
        compare(rt, array1, Collections.emptyList()).isEqualTo(-1);
        compare(rt, Collections.emptyList(), array1).isEqualTo(-1);


        var map1 = new HashMap<String,Object>();
        map1.put("lorem","ipsum");
        var map2 = new HashMap<String,Object>();
        map2.put("dolor","st");
        compare(rt, map1, map1).isEqualTo(0);
        compare(rt, map1, map2).isEqualTo(-1);
        compare(rt, Collections.emptyMap(), Collections.emptyMap()).isEqualTo(0);
        compare(rt, map1, Collections.emptyMap()).isEqualTo(-1);
        compare(rt, Collections.emptyMap(), map1).isEqualTo(-1);

        compare(rt, 1, map1).isEqualTo(-1);
        compare(rt, "foo", 1).isEqualTo(-1);
        compare(rt, 1, true).isEqualTo(-1);
        compare(rt, 0, false).isEqualTo(-1);
        compare(rt,"foo", array1).isEqualTo(-1);
        compare(rt, null, false).isEqualTo(-1);
    }

    private AbstractIntegerAssert compare(Adapter<Object> rt, Object a, Object b) {
      return assertThat(rt.compare(toAdapted(rt,a),toAdapted(rt,b)));
    }

    private void checkTypeOf(Adapter<Object> runtime, Object baseObj, JmesPathType type) {
        Object convertedObj = toAdapted(runtime,baseObj);
        assertThat(runtime.typeOf(convertedObj)).isEqualTo(type);
    }


    private Object toAdapted(Adapter<Object> runtime, Object object) {
        if (object == null) {
            return runtime.createNull();
        } else if (object instanceof Boolean) {
            return runtime.createBoolean((Boolean)object);
        } else if (object instanceof Number) {
            if (object instanceof Double || object instanceof Float) {
                return runtime.createNumber(((Number)object).doubleValue());
            } else {
                return runtime.createNumber(((Number) object).longValue());
            }
        } else if (object instanceof Map) {
            return runtime.createObject((Map<Object,Object>)object);
        } else if (object instanceof Collection) {
            return runtime.createArray((Collection)object);
        } else if (object instanceof String) {
            return runtime.createString((String)object);
        } else {
            throw new IllegalStateException(String.format("Unknown node type encountered: %s", object.getClass().getName()));
        }
    }

    private Object fromAdapted(Adapter<Object> runtime, Object object) {
        try {
            switch (runtime.typeOf(object)) {
                case ARRAY:
                    return runtime.toList(object);
                case BOOLEAN:
                    return runtime.isTruthy(object);
                case NULL:
                    return null;
                case NUMBER:
                    return runtime.toNumber(object);
                case OBJECT:
                    return parseObject(runtime, object);
                case STRING:
                    return runtime.toString(object);
                default:
                    throw new IllegalArgumentException();
            }
        } catch (IllegalStateException e ) {
            // already unboxed
            return object;
        }
    }

    private Map<String,Object> parseObject(Adapter<Object> runtime, Object obj) {
      var map = new HashMap<String,Object>();
      runtime.getPropertyNames(obj).iterator().forEachRemaining(p -> map.put((String)p,runtime.getProperty(obj,p)));
      return Collections.unmodifiableMap(map);
    }
  
}
