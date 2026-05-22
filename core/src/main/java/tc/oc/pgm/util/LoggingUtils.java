package tc.oc.pgm.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

@NullMarked
public class LoggingUtils {
  private LoggingUtils() {}

  /**
   * Logs an XML warning given the error message and specified node.
   *
   * @param logger The logger to use
   * @param message The warning message
   * @param node The node involved in the warning, if any
   */
  public static void warn(Logger logger, String message, @Nullable Node node) {
    logger.log(Level.WARNING, null, new InvalidXMLException(message, node));
  }

  public static void warn(Logger logger, String message, @Nullable Element element) {
    warn(logger, message, Node.fromNullable(element));
  }

  public static void warn(Logger logger, String message, @Nullable Attribute element) {
    warn(logger, message, Node.fromNullable(element));
  }
}
