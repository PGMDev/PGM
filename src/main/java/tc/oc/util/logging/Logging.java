package tc.oc.util.logging;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.simple.SimpleLoggerContextFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import tc.oc.util.reflect.ReflectionUtils;

public class Logging {

  private static final Map<Level, String> JUL_ABBREVS =
      ImmutableMap.<Level, String>builder()
          .put(Level.ALL, "A")
          .put(Level.FINEST, "F")
          .put(Level.FINER, "F")
          .put(Level.FINE, "F")
          .put(Level.CONFIG, "C")
          .put(Level.INFO, "I")
          .put(Level.WARNING, "W")
          .put(Level.SEVERE, "E")
          .put(Level.OFF, "O")
          .build();
  private static final Map<org.apache.logging.log4j.Level, String> L4J_ABBREVS =
      ImmutableMap.<org.apache.logging.log4j.Level, String>builder()
          .put(org.apache.logging.log4j.Level.ALL, "A")
          .put(org.apache.logging.log4j.Level.TRACE, "T")
          .put(org.apache.logging.log4j.Level.DEBUG, "D")
          .put(org.apache.logging.log4j.Level.INFO, "I")
          .put(org.apache.logging.log4j.Level.WARN, "W")
          .put(org.apache.logging.log4j.Level.FATAL, "E")
          .put(org.apache.logging.log4j.Level.OFF, "O")
          .build();
  private static final Map<Level, ChatColor> JUL_COLORS =
      ImmutableMap.<Level, ChatColor>builder()
          .put(Level.ALL, ChatColor.GREEN)
          .put(Level.FINEST, ChatColor.AQUA)
          .put(Level.FINER, ChatColor.AQUA)
          .put(Level.FINE, ChatColor.AQUA)
          .put(Level.CONFIG, ChatColor.AQUA)
          .put(Level.INFO, ChatColor.BLUE)
          .put(Level.WARNING, ChatColor.YELLOW)
          .put(Level.SEVERE, ChatColor.RED)
          .build();
  private static final Map<org.apache.logging.log4j.Level, ChatColor> L4J_COLORS =
      ImmutableMap.<org.apache.logging.log4j.Level, ChatColor>builder()
          .put(org.apache.logging.log4j.Level.ALL, ChatColor.GREEN)
          .put(org.apache.logging.log4j.Level.TRACE, ChatColor.AQUA)
          .put(org.apache.logging.log4j.Level.DEBUG, ChatColor.AQUA)
          .put(org.apache.logging.log4j.Level.INFO, ChatColor.BLUE)
          .put(org.apache.logging.log4j.Level.WARN, ChatColor.YELLOW)
          .put(org.apache.logging.log4j.Level.FATAL, ChatColor.RED)
          .build();

  private static LoggingConfig loggingConfig;

  private Logging() {}

  public static <T> T getParam(LogRecord record, Class<T> type) {
    if (record.getParameters() != null) {
      for (Object param : record.getParameters()) {
        if (type.isInstance(param)) return type.cast(param);
      }
    }
    return null;
  }

  public static void addParam(LogRecord record, Object param) {
    Object[] params = record.getParameters();
    if (params == null) {
      params = new Object[] {param};
    } else {
      Object[] oldParams = params;
      params = new Object[oldParams.length + 1];
      System.arraycopy(oldParams, 0, params, 0, oldParams.length);
      params[oldParams.length] = param;
    }
    record.setParameters(params);
  }

  public static boolean isError(LogRecord record) {
    return record.getLevel().intValue() >= Level.WARNING.intValue();
  }

  public static Logger getRootLogger(Logger logger) {
    while (logger.getParent() != null) logger = logger.getParent();
    return logger;
  }

  public static Level getEffectiveLevel(Logger logger) {
    if (logger.getLevel() != null) {
      return logger.getLevel();
    }

    if (logger.getParent() != null) {
      return getEffectiveLevel(logger.getParent());
    }

    return null;
  }

  /**
   * Get the JUL logging properties using dark magic. Throws if this fails, possibly due to
   * incorrect assumptions about the private implementation of {@link LogManager}.
   */
  public static Properties getLoggingProperties()
      throws IllegalAccessException, NoSuchFieldException {
    Field field = LogManager.class.getDeclaredField("props");
    field.setAccessible(true);
    Object value = field.get(LogManager.getLogManager());
    if (value instanceof Properties) {
      return (Properties) value;
    }
    return null;
  }

  public static void updateFromLoggingProperties()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = LogManager.class.getDeclaredMethod("setLevelsOnExistingLoggers");
    method.setAccessible(true);
    method.invoke(LogManager.getLogManager());
  }

  /**
   * Set the default level for the given logger name. This will affect any current or future logger
   * with that name. This is equivalent to setting the level through the logging properties file.
   */
  public static void setDefaultLevel(String loggerName, Level level)
      throws NoSuchFieldException, IllegalAccessException {
    getLoggingProperties().setProperty(loggerName + ".level", level.getName());
  }

  public static @Nullable Logger findLogger(String search) {
    LogManager lm = LogManager.getLogManager();
    if ("".equals(search)) return lm.getLogger("");
    String name = findLogger(search, Collections.list(lm.getLoggerNames()));
    return name == null ? null : lm.getLogger(name);
  }

  public static @Nullable String findLogger(String search, Collection<String> names) {
    String searchLower = search.toLowerCase();

    String bestName = null;
    int bestScore = 0;

    for (String name : names) {
      String nameLower = name.toLowerCase();
      int pos = nameLower.indexOf(searchLower);
      if (pos == -1) continue;

      int score = 0;
      if (pos == 0) score += 2; // match at start
      if (pos + searchLower.length() == nameLower.length()) score += 1; // match at finish

      if (bestName != null) {
        if (score < bestScore) continue;
        if (score == bestScore && name.length() >= bestName.length()) continue;
      }

      bestName = name;
      bestScore = score;
    }

    return bestName;
  }

  public static String formatLegacy(LogRecord record) {
    return ChatColor.DARK_GRAY
        + "["
        + levelColor(record.getLevel())
        + levelAbbrev(record.getLevel())
        + ChatColor.DARK_GRAY
        + "] "
        + ChatColor.GRAY
        + record.getMessage();
  }

  public static ChatColor levelColor(Level level) {
    if (level == null) {
      return ChatColor.DARK_GRAY;
    } else if (JUL_COLORS.containsKey(level)) {
      return JUL_COLORS.get(level);
    } else {
      return ChatColor.LIGHT_PURPLE;
    }
  }

  public static ChatColor levelColor(org.apache.logging.log4j.Level level) {
    if (level == null) {
      return ChatColor.DARK_GRAY;
    } else if (L4J_COLORS.containsKey(level)) {
      return L4J_COLORS.get(level);
    } else {
      return ChatColor.LIGHT_PURPLE;
    }
  }

  public static String levelAbbrev(Level level) {
    if (level != null && JUL_ABBREVS.containsKey(level)) {
      return JUL_ABBREVS.get(level);
    } else {
      return "?";
    }
  }

  public static String levelAbbrev(org.apache.logging.log4j.Level level) {
    if (level != null && L4J_ABBREVS.containsKey(level)) {
      return L4J_ABBREVS.get(level);
    } else {
      return "?";
    }
  }

  /**
   * Fuck L4J in its tight asshole for leaving every useful method out of its public API, and
   * forcing me to write all this garbage.
   */
  public static class L4J {
    public static List<? extends LoggerContext> getContexts() {
      LoggerContextFactory factory = org.apache.logging.log4j.LogManager.getFactory();
      if (factory instanceof SimpleLoggerContextFactory) {
        return Collections.singletonList(factory.getContext(null, null, true, null));
      }
      return ((Log4jContextFactory) org.apache.logging.log4j.LogManager.getFactory())
          .getSelector()
          .getLoggerContexts();
    }

    public static String getContextName(LoggerContext context) {
      if (context instanceof org.apache.logging.log4j.core.LoggerContext) {
        return ((org.apache.logging.log4j.core.LoggerContext) context).getName();
      } else {
        return context.getClass().getSimpleName();
      }
    }

    public static Iterable<org.apache.logging.log4j.Logger> getLoggers() {
      return Iterables.concat(
          Iterables.transform(
              getContexts(),
              new Function<LoggerContext, Iterable<org.apache.logging.log4j.Logger>>() {
                @Override
                public Iterable<org.apache.logging.log4j.Logger> apply(
                    @Nullable LoggerContext context) {
                  return getLoggers(context).values();
                }
              }));
    }

    public static Map<String, org.apache.logging.log4j.Logger> getLoggers(LoggerContext context) {
      return ReflectionUtils.readField(context.getClass(), context, Map.class, "loggers");
    }

    public static @Nullable org.apache.logging.log4j.Logger findLogger(String search) {
      for (LoggerContext context : getContexts()) {
        String name = Logging.findLogger(search, getLoggers(context).keySet());
        if (name != null) return (org.apache.logging.log4j.core.Logger) context.getLogger(name);
      }
      return null;
    }

    public static org.apache.logging.log4j.Level getLevel(org.apache.logging.log4j.Logger logger) {
      if (logger instanceof org.apache.logging.log4j.core.Logger) {
        return ((org.apache.logging.log4j.core.Logger) logger).getLevel();
      } else if (logger instanceof SimpleLogger) {
        return ((SimpleLogger) logger).getLevel();
      } else {
        return null;
      }
    }

    public static void setLevel(
        org.apache.logging.log4j.Logger logger, org.apache.logging.log4j.Level level) {
      if (logger instanceof org.apache.logging.log4j.core.Logger) {
        ((org.apache.logging.log4j.core.Logger) logger).setLevel(level);
      } else if (logger instanceof SimpleLogger) {
        ((SimpleLogger) logger).setLevel(level);
      }
    }

    public static org.apache.logging.log4j.Logger getParent(
        org.apache.logging.log4j.Logger logger) {
      if (logger instanceof org.apache.logging.log4j.core.Logger) {
        return ((org.apache.logging.log4j.core.Logger) logger).getParent();
      } else {
        return null;
      }
    }

    public static org.apache.logging.log4j.Level getEffectiveLevel(
        org.apache.logging.log4j.Logger logger) {
      org.apache.logging.log4j.Level level = getLevel(logger);
      if (level != null) return level;

      org.apache.logging.log4j.Logger parent = getParent(logger);
      if (parent != null) return getEffectiveLevel(parent);

      return null;
    }
  }

  public static LoggingConfig getLoggingConfig() {
    return loggingConfig;
  }

  public static void setLoggingConfig(LoggingConfig loggingConfig) {
    Logging.loggingConfig = loggingConfig;
    loggingConfig.load();
  }
}
