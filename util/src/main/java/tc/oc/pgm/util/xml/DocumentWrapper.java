package tc.oc.pgm.util.xml;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.function.Consumer;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

public class DocumentWrapper extends Document {

  private static final Set<String> IGNORED = Sets.newHashSet("variant", "tutorial", "edition");

  private boolean visitingAllowed = true;

  public DocumentWrapper() {
    super();
  }

  public DocumentWrapper(Element rootElement, DocType docType, String baseURI) {
    super(rootElement, docType, baseURI);
  }

  public DocumentWrapper(Element rootElement, DocType docType) {
    super(rootElement, docType);
  }

  public DocumentWrapper(Element rootElement) {
    super(rootElement);
  }

  public void setVisitingAllowed(boolean visitingAllowed) {
    this.visitingAllowed = visitingAllowed;
  }

  public void runWithoutVisitation(DocumentWorker worker) throws InvalidXMLException {
    this.visitingAllowed = false;
    worker.run();
    this.visitingAllowed = true;
  }

  public boolean isVisitingAllowed() {
    return visitingAllowed;
  }

  public void checkUnvisited(Consumer<Node> unvisited) {
    checkVisited(getRootElement(), unvisited);
  }

  private void checkVisited(Element el, Consumer<Node> unvisited) {
    for (Attribute attribute : el.getAttributes()) {
      if (attribute.getNamespace() == Namespace.NO_NAMESPACE
          && !((VisitableAttribute) attribute).wasVisited())
        unvisited.accept(Node.fromNullable(attribute));
    }

    for (int i = 0; i < el.getContentSize(); i++) {
      Content c = el.getContent(i);
      if (!(c instanceof InheritingElement)) continue;
      InheritingElement child = (InheritingElement) c;

      if (child.getNamespace() == Namespace.NO_NAMESPACE && !IGNORED.contains(child.getName())) {
        if (!child.wasVisited()) unvisited.accept(Node.fromNullable(child));
        else checkVisited(child, unvisited);
      }
    }
  }

  public interface DocumentWorker {
    void run() throws InvalidXMLException;
  }
}
