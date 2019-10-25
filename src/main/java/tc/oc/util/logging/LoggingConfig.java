package tc.oc.util.logging;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class LoggingConfig {

  protected final ClassLogger logger;

  public LoggingConfig(Logger logger) {
    this.logger = ClassLogger.get(logger, getClass());
  }

  public abstract Map<String, Object> getConfig();

  public void load() {
    Logger.getLogger("").setLevel(Level.INFO);
    try {
      for (Map.Entry<String, Object> entry : getConfig().entrySet()) {
        String[] parts = entry.getKey().split("\\.");
        if (parts.length == 2 && "level".equals(parts[1])) {
          String loggerName = parts[0];
          String levelName = (String) entry.getValue();
          if ("root".equals(loggerName)) {
            loggerName = "";
          } else {
            loggerName = loggerName.replace('-', '.');
          }
          Logging.setDefaultLevel(loggerName, Level.parse(levelName));
        }
      }

      Logging.updateFromLoggingProperties();
    } catch (NoSuchFieldException
        | NoSuchMethodException
        | IllegalAccessException
        | InvocationTargetException e) {
      logger.log(Level.WARNING, "Reflection error applying logging config", e);
    }
  }
}
