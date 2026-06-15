package tc.oc.pgm.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.jdom2.Element;
import tc.oc.pgm.util.function.ThrowingFunction;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public record MethodParsers<T>(
    Object actualParser,
    Function<Element, String> keyExtractor,
    Map<String, Method> methods,
    ThrowingFunction<Element, T, InvalidXMLException> fallback) {
  private static final Map<Class<?>, Map<String, Method>> METHOD_CACHE = new ConcurrentHashMap<>();

  public T parse(Element el) throws InvalidXMLException {
    var key = keyExtractor.apply(el);
    Method parser = key == null ? null : methods.get(key.toLowerCase(Locale.ROOT));
    if (parser == null) return fallback.apply(el);
    try {
      //noinspection unchecked
      return (T) parser.invoke(actualParser, el);
    } catch (Exception e) {
      throw InvalidXMLException.coerce(e, new Node(el));
    }
  }

  public T parse(Element el, Object... additionalParams) throws InvalidXMLException {
    var key = keyExtractor.apply(el);
    Method parser = key == null ? null : methods.get(key.toLowerCase(Locale.ROOT));
    if (parser == null) return fallback.apply(el);
    try {
      Object[] params = new Object[additionalParams.length + 1];
      params[0] = el;
      System.arraycopy(additionalParams, 0, params, 1, additionalParams.length);
      //noinspection unchecked
      return (T) parser.invoke(actualParser, params);
    } catch (Exception e) {
      throw InvalidXMLException.coerce(e, new Node(el));
    }
  }

  public MethodParsers<T> withFallback(ThrowingFunction<Element, T, InvalidXMLException> fallback) {
    return new MethodParsers<>(actualParser, keyExtractor, methods, fallback);
  }

  private static <T> MethodParsers<T> of(
      Object actualParser, Function<Element, String> keyExtractor, String name) {
    return new MethodParsers<>(
        actualParser, keyExtractor, getMethodParsersForClass(actualParser.getClass()), el -> {
          throw new InvalidXMLException("Unknown " + name + " type: " + keyExtractor.apply(el), el);
        });
  }

  public static <T> MethodParsers<T> byNameParser(Object actualParser, String type) {
    return of(actualParser, Element::getName, type);
  }

  public static <T> MethodParsers<T> byAttrParser(Object actualParser, String attribute) {
    return of(actualParser, el -> el.getAttributeValue(attribute), attribute);
  }

  private static Map<String, Method> getMethodParsersForClass(Class<?> cls) {
    return METHOD_CACHE.computeIfAbsent(cls, key -> {
      Map<String, Method> parsers = new HashMap<>();
      for (Method method : key.getMethods()) {
        MethodParser parser = method.getAnnotation(MethodParser.class);
        if (parser != null) {
          parsers.put(parser.value().toLowerCase(Locale.ROOT), method);
        }
      }
      return Map.copyOf(parsers);
    });
  }
}
