package tc.oc.pgm.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.lang.reflect.Method;
import java.util.Map;

public class MethodParsers {
  public static Map<String, Method> getMethodParsersForClass(Class<?> cls) {
    Map<String, Method> parsers = Maps.newHashMap();

    for (Method method : cls.getMethods()) {
      MethodParser parser = method.getAnnotation(MethodParser.class);
      if (parser != null) {
        parsers.put(parser.value(), method);
      }
    }

    return ImmutableMap.copyOf(parsers);
  }
}
