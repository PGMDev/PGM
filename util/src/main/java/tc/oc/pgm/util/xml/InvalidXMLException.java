package tc.oc.pgm.util.xml;

import java.lang.reflect.InvocationTargetException;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.JDOMParseException;
import org.jetbrains.annotations.Nullable;

public class InvalidXMLException extends Exception {

  private final @Nullable Node node;
  private final @Nullable Document document;
  private final @Nullable String documentPath;
  private final int startLine, endLine, column;

  protected InvalidXMLException(
      String message,
      @Nullable Node node,
      @Nullable Document document,
      @Nullable String documentPath,
      int startLine,
      int endLine,
      int column,
      Throwable cause) {
    super(message, cause);

    this.node = node;
    this.document = document != null ? document : node != null ? node.getDocument() : null;

    this.documentPath =
        documentPath != null
            ? documentPath
            : this.document != null ? this.document.getBaseURI() : null;
    this.startLine = startLine > 0 ? startLine : this.node != null ? this.node.getStartLine() : 0;
    this.endLine = endLine > 0 ? endLine : this.node != null ? this.node.getEndLine() : 0;
    this.column = column > 0 ? column : this.node != null ? this.node.getColumn() : 0;
  }

  public InvalidXMLException(@Nullable Node node, Throwable cause) {
    this("", node, cause);
  }

  public InvalidXMLException(String message, @Nullable Node node, Throwable cause) {
    this(message, node, null, null, 0, 0, 0, cause);
  }

  public InvalidXMLException(String message, @Nullable Document document, Throwable cause) {
    this(message, null, document, null, 0, 0, 0, cause);
  }

  public InvalidXMLException(String message, @Nullable String documentPath, Throwable cause) {
    this(message, null, null, documentPath, 0, 0, 0, cause);
  }

  public InvalidXMLException(String message, @Nullable Element element, Throwable cause) {
    this(message, element == null ? null : new Node(element), cause);
  }

  public InvalidXMLException(String message, @Nullable Attribute attribute, Throwable cause) {
    this(message, attribute == null ? null : new Node(attribute), cause);
  }

  public InvalidXMLException(String message, Document document) {
    this(message, document, null);
  }

  public InvalidXMLException(String message, String documentPath) {
    this(message, documentPath, null);
  }

  public InvalidXMLException(String message, Node node) {
    this(message, node, null);
  }

  public InvalidXMLException(String message, Element element) {
    this(message, element, null);
  }

  public InvalidXMLException(String message, Attribute attribute) {
    this(message, attribute, null);
  }

  public static InvalidXMLException fromJDOM(JDOMParseException e, String documentPath) {
    return new InvalidXMLException(
        e.getMessage(),
        null,
        e.getPartialDocument(),
        documentPath,
        e.getLineNumber(),
        e.getLineNumber(),
        e.getColumnNumber(),
        e);
  }

  /** Try to extract an InvalidXMLException from the given Throwable, otherwise wrap it in one. */
  public static InvalidXMLException coerce(Throwable e) {
    return coerce(e, null);
  }

  public static InvalidXMLException coerce(Throwable e, @Nullable Node node) {
    if (e instanceof InvocationTargetException) {
      return coerce(e.getCause(), node);
    } else if (e.getCause() instanceof InvalidXMLException) {
      return (InvalidXMLException) e.getCause();
    } else if (e instanceof InvalidXMLException) {
      return (InvalidXMLException) e;
    } else {
      return new InvalidXMLException("Unhandled " + e.getClass().getName(), node, e);
    }
  }

  public @Nullable Node getNode() {
    return node;
  }

  public @Nullable Document getDocument() {
    return document;
  }

  public @Nullable String getDocumentPath() {
    return documentPath;
  }

  public int getStartLine() {
    return startLine;
  }

  public int getEndLine() {
    return endLine;
  }

  public int getColumn() {
    return column;
  }

  public @Nullable String getWhere() {
    if (startLine > 0) {
      if (startLine == endLine) {
        if (column > 0) {
          return "line " + startLine + ", column " + column;
        }
        return "line " + startLine;
      }
      return "line " + startLine + " to " + endLine;
    }
    return null;
  }

  public @Nullable String getWhatAndWhere() {
    String what = getNode() == null ? null : getNode().getDescription();
    String where = getWhere();

    if (what != null) {
      if (where != null) {
        return what + " @ " + where;
      }
      return what;
    } else if (where != null) {
      return where;
    }
    return null;
  }

  public @Nullable String getFullLocation() {
    String path = getDocumentPath();
    String location = getWhatAndWhere();
    if (path != null) {
      return location == null ? path : path + " - " + location;
    }
    return location;
  }
}
