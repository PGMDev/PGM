package tc.oc.util.bukkit;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gson.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import org.joda.time.Duration;
import tc.oc.util.Numbers;
import tc.oc.util.TimeUtils;
import tc.oc.util.components.PeriodFormats;

public abstract class ConfigUtils {

  public static <T extends Enum<T>> T getEnum(
      ConfigurationSection config, String key, Class<T> type, @Nullable T def) {
    final String value = config.getString(key);
    return value == null ? def : Enum.valueOf(type, value.toUpperCase());
  }

  public static <T extends Enum<T>> T getEnum(
      ConfigurationSection config, String key, Class<T> type) {
    return getEnum(config, key, type, null);
  }

  public static @Nullable Duration getDuration(ConfigurationSection config, String key) {
    return getDuration(config, key, null);
  }

  public static Duration getDuration(ConfigurationSection config, String key, Duration def) {
    String str = config.getString(key);
    if (str == null) {
      return def;
    } else if ("oo".equals(str)) {
      return TimeUtils.INFINITE_DURATION;
    } else {
      return PeriodFormats.SHORTHAND.parsePeriod(str).toStandardDuration();
    }
  }

  public static double getPercentage(ConfigurationSection config, String key, double def) {
    double percent = config.getDouble(key, def);
    if (percent < 0 || percent > 1) {
      throw new IllegalArgumentException(
          "Config value " + key + ": percentage must be between 0 and 1");
    }
    return percent;
  }

  public static Vector getVector(ConfigurationSection section, String key, Vector def) {
    Object o = section.get(key);
    if (o == null) {
      return def;
    } else if (o instanceof List) {
      List v = (List) o;
      return new Vector(
          Numbers.coerce(v.get(0), Double.class, true),
          Numbers.coerce(v.get(1), Double.class, true),
          Numbers.coerce(v.get(2), Double.class, true));
    } else if (o instanceof ConfigurationSection) {
      ConfigurationSection v = (ConfigurationSection) o;
      return new Vector(v.getDouble("x"), v.getDouble("y"), v.getDouble("z"));
    } else {
      throw new IllegalArgumentException("Cannot coerce " + o.getClass() + " to vector");
    }
  }

  public static Path getPath(ConfigurationSection section, String key, Path def) {
    String value = section.getString(key);
    return value == null ? def : Paths.get(value);
  }

  public static List<Path> getPathList(ConfigurationSection section, String key) {
    return Lists.transform(
        section.getStringList(key),
        new Function<String, Path>() {
          @Override
          public Path apply(String path) {
            return Paths.get(path);
          }
        });
  }

  public static URL getUrl(ConfigurationSection section, String key, URL def) {
    String value = section.getString(key);
    try {
      return value == null ? def : new URL(value);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid URL format '" + value + "'", e);
    }
  }

  public static ConfigurationSection getOrCreateSection(
      ConfigurationSection config, String key, Map<?, ?> values) {
    ConfigurationSection section = config.getConfigurationSection(key);
    if (section == null) {
      section = config.createSection(key, values);
    }
    return section;
  }

  public static ConfigurationSection getOrCreateSection(ConfigurationSection config, String key) {
    return getOrCreateSection(config, key, Collections.emptyMap());
  }

  private static JsonElement toJson(Object value) {
    if (value instanceof ConfigurationSection) {
      return toJson((ConfigurationSection) value);
    } else if (value instanceof Map) {
      return toJson((Map) value);
    } else if (value instanceof List) {
      return toJson((List) value);
    } else if (value instanceof String) {
      return new JsonPrimitive((String) value);
    } else if (value instanceof Character) {
      return new JsonPrimitive((Character) value);
    } else if (value instanceof Number) {
      return new JsonPrimitive((Number) value);
    } else if (value instanceof Boolean) {
      return new JsonPrimitive((Boolean) value);
    } else if (value == null) {
      return JsonNull.INSTANCE;
    } else {
      throw new IllegalArgumentException(
          "Cannot coerce " + value.getClass().getSimpleName() + " to JSON");
    }
  }

  public static JsonArray toJson(List list) {
    JsonArray json = new JsonArray();
    for (Object item : list) {
      json.add(toJson(item));
    }
    return json;
  }

  public static JsonObject toJson(Map<?, ?> map) {
    JsonObject json = new JsonObject();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      json.add(entry.getKey().toString(), toJson(entry.getValue()));
    }
    return json;
  }

  public static JsonObject toJson(ConfigurationSection config) {
    JsonObject json = new JsonObject();
    for (Map.Entry<String, Object> entry : config.getValues(false).entrySet()) {
      json.add(entry.getKey(), toJson(entry.getValue()));
    }
    return json;
  }
}
