package tc.oc.server;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import org.bukkit.Bukkit;
import tc.oc.util.logging.Logging;

public class LogHandler extends Handler {

  protected final String permission;

  public LogHandler(String permission) {
    this.permission = permission;
  }

  protected void publishTrace(Throwable thrown) {
    thrown.printStackTrace();
  }

  protected String formatMessage(LogRecord record) {
    return Logging.formatLegacy(record);
  }

  @Override
  public void publish(LogRecord record) {
    Bukkit.broadcast(formatMessage(record), permission);

    if (Bukkit.getConsoleSender().hasPermission(permission) && record.getThrown() != null) {
      publishTrace(record.getThrown());
    }
  }

  @Override
  public void flush() {}

  @Override
  public void close() throws SecurityException {}
}
