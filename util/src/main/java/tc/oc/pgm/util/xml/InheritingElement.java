package tc.oc.pgm.util.xml;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.located.LocatedElement;

/**
 * Deep-copies a given {@link Element} and inherits attributes from its parent {@link Element}, if
 * there is one.
 *
 * <p>Unlike a {@link #clone}d element, calling {@link #getParent} or {@link #getDocument} on this
 * element will return the same value as the original element, even though this element is not
 * actually a child of the original element's parent. This is needed so that copied elements know
 * which document they came from, and can generate proper error messages.
 */
public class InheritingElement extends LocatedElement {

  private AtomicBoolean visited = new AtomicBoolean();
  private String originalUri;
  private int startLine, endLine;
  private int indexInParent = Integer.MIN_VALUE;

  public InheritingElement(Element el) {
    super(el.getName(), el.getNamespace());
    setParent(el.getParent());

    final InheritingElement bounded = (InheritingElement) el;
    setLine(bounded.getLine());
    setColumn(bounded.getColumn());
    setStartLine(bounded.getStartLine());
    setEndLine(bounded.getEndLine());
    this.indexInParent = bounded.indexInParent();
    this.originalUri = bounded.originalUri;
    this.visited = bounded.visited;

    setContent(el.cloneContent());

    for (Attribute attribute : el.getAttributes()) {
      setAttribute(attribute.clone());
    }

    if (getParent() instanceof Element) {
      for (Attribute attribute : ((Element) el.getParent()).getAttributes()) {
        if (getAttribute(attribute.getName()) == null) {
          setAttribute(attribute.clone());
        }
      }
    }
  }

  protected InheritingElement(String name, Namespace namespace) {
    super(name, namespace);
  }

  protected InheritingElement(String name) {
    super(name);
  }

  protected InheritingElement(String name, String uri) {
    super(name, uri);
  }

  protected InheritingElement(String name, String prefix, String uri) {
    super(name, prefix, uri);
  }

  public int getStartLine() {
    return startLine;
  }

  protected void setStartLine(int startLine) {
    setLine(startLine);
    this.startLine = startLine;
  }

  public int getEndLine() {
    return endLine;
  }

  protected void setEndLine(int endLine) {
    this.endLine = endLine;
  }

  protected int indexInParent() {
    if (indexInParent < -1) {
      final Element parent = getParentElement();
      indexInParent = parent == null ? -1 : parent.indexOf(this);
    }
    return indexInParent;
  }

  public boolean wasVisited() {
    return visited.get();
  }

  private void setVisited() {
    visited.set(true);
  }

  @Override
  public List<Element> getChildren() {
    List<Element> children = super.getChildren();
    if (visitingAllowed()) children.forEach(child -> ((InheritingElement) child).setVisited());
    return children;
  }

  public Iterable<Element> getChildren(Set<String> names) {
    boolean visitingAllowed = visitingAllowed();
    return Iterables.filter(
        super.getChildren(),
        el -> {
          if (names.contains(el.getName())) {
            if (visitingAllowed) ((InheritingElement) el).setVisited();
            return true;
          }
          return false;
        });
  }

  @Override
  public List<Element> getChildren(String cname, Namespace ns) {
    List<Element> children = super.getChildren(cname, ns);
    if (visitingAllowed()) children.forEach(child -> ((InheritingElement) child).setVisited());
    return children;
  }

  @Override
  public Element getChild(String cname, Namespace ns) {
    Element child = super.getChild(cname, ns);
    if (visitingAllowed() && child != null) ((InheritingElement) child).setVisited();
    return child;
  }

  private boolean visitingAllowed() {
    return ((DocumentWrapper) getDocument()).isVisitingAllowed();
  }

  public String getOriginalUri() {
    return originalUri;
  }

  @Override
  public InheritingElement clone() {
    final InheritingElement that = (InheritingElement) super.clone();
    that.setLine(getLine());
    that.setColumn(getColumn());
    that.setStartLine(getStartLine());
    that.setEndLine(getEndLine());
    that.visited = this.visited;
    that.originalUri = getDocument().getBaseURI();
    return that;
  }
}
