package tc.oc.xml;

import tc.oc.util.Version;

public class InvalidXMLVersionException extends InvalidXMLException {

  private final Version version;

  public InvalidXMLVersionException(Node node, Version version) {
    super("Unsupported protocol version", node);
    this.version = version;
  }

  public Version getVersion() {
    return version;
  }
}
