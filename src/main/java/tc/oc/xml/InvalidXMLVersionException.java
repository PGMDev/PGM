package tc.oc.xml;

import tc.oc.util.SemanticVersion;

public class InvalidXMLVersionException extends InvalidXMLException {

  private final SemanticVersion version;

  public InvalidXMLVersionException(Node node, SemanticVersion version) {
    super("Unsupported protocol version", node);
    this.version = version;
  }

  public SemanticVersion getVersion() {
    return version;
  }
}
