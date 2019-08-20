package io.burt.jmespath.vertx;

import io.burt.jmespath.JmesPathRuntimeTest;
import io.burt.jmespath.RuntimeConfiguration;
import io.burt.jmespath.Adapter;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

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
                    Instant.parse("2019-08-19T14:15:00.000Z"), // 7
                    Arrays.asList(new Object[]{"hi"}),         // 8
                    map,                                       // 9
                    new StringBuffer("testing"),               // 10
                    null                                       // 11
        );

        var runtime = createRuntime(RuntimeConfiguration.defaultConfiguration());
        var newList = runtime.toList(runtime.createArray(backingArr));

        assertThat(newList).isNotEqualTo(backingArr);
        assertThat(newList.get(0)).isEqualTo(1L);
        assertThat(newList.get(1)).isEqualTo(1f);
        assertThat(newList.get(2)).isEqualTo(1);
        assertThat(newList.get(3)).isEqualTo(1.2);
        assertThat(newList.get(4)).isEqualTo(bytesString);
        assertThat(newList.get(5)).isEqualTo("");
        assertThat(newList.get(6)).isEqualTo(true);
        assertThat(newList.get(7)).isEqualTo(Instant.parse("2019-08-19T14:15:00.000Z").toString());
        assertThat(runtime.toList(newList.get(8))).isEqualTo(backingArr.get(8));
        assertThat(parseObject(runtime,(newList.get(9)))).isEqualTo(backingArr.get(9));
        assertThat(newList.get(10)).isNotNull();
        assertThat(newList.get(11)).isNull();
    }

    private Map<String,Object> parseObject(Adapter<Object> runtime, Object obj) {
      var map = new HashMap<String,Object>();
      runtime.getPropertyNames(obj).iterator().forEachRemaining(p -> map.put((String)p,runtime.getProperty(obj,p)));
      return Collections.unmodifiableMap(map);
    }
  
}
