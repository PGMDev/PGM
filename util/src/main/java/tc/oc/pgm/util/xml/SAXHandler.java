package tc.oc.pgm.util.xml;

import org.jdom2.Attribute;
import org.jdom2.AttributeType;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.sax.SAXHandlerFactory;
import org.jdom2.located.LocatedJDOMFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class SAXHandler extends org.jdom2.input.sax.SAXHandler {
  public static final SAXHandlerFactory FACTORY = factory -> new SAXHandler();

  private SAXHandler() {
    super(new JDOMFactory());
  }

  @Override
  public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
      throws SAXException {
    super.startElement(namespaceURI, localName, qName, atts);
    ((InheritingElement) getCurrentElement()).setStartLine(getDocumentLocator().getLineNumber());
  }

  @Override
  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    ((InheritingElement) getCurrentElement()).setEndLine(getDocumentLocator().getLineNumber());
    super.endElement(namespaceURI, localName, qName);
  }

  private static class JDOMFactory extends LocatedJDOMFactory {
    @Override
    public Document document(Element rootElement, DocType docType) {
      return new DocumentWrapper(rootElement, docType);
    }

    @Override
    public Document document(Element rootElement, DocType docType, String baseURI) {
      return new DocumentWrapper(rootElement, docType, baseURI);
    }

    @Override
    public Document document(Element rootElement) {
      return new DocumentWrapper(rootElement);
    }

    @Override
    public Element element(int line, int col, String name, Namespace namespace) {
      return new InheritingElement(name, namespace);
    }

    @Override
    public Element element(int line, int col, String name) {
      return new InheritingElement(name);
    }

    @Override
    public Element element(int line, int col, String name, String uri) {
      return new InheritingElement(name, uri);
    }

    @Override
    public Element element(int line, int col, String name, String prefix, String uri) {
      return new InheritingElement(name, prefix, uri);
    }

    @Override
    public Attribute attribute(String name, String value, Namespace namespace) {
      return new VisitableAttribute(name, value, namespace);
    }

    @Override
    @Deprecated
    public Attribute attribute(String name, String value, int type, Namespace namespace) {
      return new VisitableAttribute(name, value, AttributeType.byIndex(type), namespace);
    }

    @Override
    public Attribute attribute(String name, String value, AttributeType type, Namespace namespace) {
      return new VisitableAttribute(name, value, type, namespace);
    }

    @Override
    public Attribute attribute(String name, String value) {
      return new VisitableAttribute(name, value);
    }

    @Override
    @Deprecated
    public Attribute attribute(String name, String value, int type) {
      return new VisitableAttribute(name, value, type);
    }

    @Override
    public Attribute attribute(String name, String value, AttributeType type) {
      return new VisitableAttribute(name, value, type);
    }
  }
}
