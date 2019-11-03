package tc.oc.pgm.map;

import java.util.logging.LogRecord;
import org.jdom2.input.JDOMParseException;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.server.LogHandler;
import tc.oc.xml.InvalidXMLException;

public class MapLogHandler extends LogHandler {

  public MapLogHandler() {
    super(Permissions.DEBUG);
  }

  @Override
  protected void publishTrace(Throwable thrown) {
    while (thrown instanceof ModuleLoadException || thrown instanceof InvalidXMLException)
      thrown = thrown.getCause();
    if (thrown != null && !(thrown instanceof JDOMParseException)) {
      super.publishTrace(thrown);
    }
  }

  @Override
  protected String formatMessage(LogRecord record) {
    if (record instanceof PGMMap.MapLogRecord) {
      return ((PGMMap.MapLogRecord) record).getLegacyFormattedMessage();
    } else {
      return super.formatMessage(record);
    }
  }
}
