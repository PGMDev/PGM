package tc.oc.pgm.util.xml;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.Assert.assertTrue;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.located.Located;
import org.jetbrains.annotations.Nullable;

/**
 * A hybrid wrapper for either an {@link Element} or an {@link Attribute}, enabling both of them to
 * be handled in a generic way.
 */
public class Node {

  private final Object node;

  public Node(Element element) {
    this.node = assertNotNull(element);
  }

  public Node(Attribute attribute) {
    this.node = assertNotNull(attribute);
  }

  public String getName() {
    if (this.node instanceof Attribute) {
      return ((Attribute) this.node).getName();
    } else {
      return ((Element) this.node).getName();
    }
  }

  /** Gets the exact text content of the element or attribute */
  public String getValue() {
    if (this.node instanceof Attribute) {
      return ((Attribute) this.node).getValue();
    } else {
      return ((Element) this.node).getText();
    }
  }

  /**
   * If this Node is wrapping an Attribute, returns the same as {@link #getValue()}. If this Node is
   * wrapping an Element, returns {@link Element#getTextNormalize()}.
   */
  public String getValueNormalize() {
    if (this.node instanceof Attribute) {
      return ((Attribute) this.node).getValue();
    } else {
      return ((Element) this.node).getTextNormalize();
    }
  }

  public boolean isAttribute() {
    return this.node instanceof Attribute;
  }

  public boolean isElement() {
    return this.node instanceof Element;
  }

  public Attribute getAttribute() {
    return (Attribute) this.node;
  }

  public Element getElement() {
    return (Element) this.node;
  }

  public String getDocumentPath() {
    if (this.node instanceof Attribute) {
      return getDocumentPath(getAttribute().getParent());
    } else {
      return getDocumentPath(getElement());
    }
  }

  private static String getDocumentPath(Element el) {
    InheritingElement inheritingEl = (InheritingElement) el;

    String original = inheritingEl.getOriginalUri();
    String current = inheritingEl.getDocument().getBaseURI();
    return current + (original == null || current.equals(original) ? "" : original);
  }

  private static String describe(Element el) {
    Element parent = el.getParentElement();
    if (parent != null && !parent.isRootElement())
      return "'" + parent.getName() + "' > '" + el.getName() + "' element";
    return "'" + el.getName() + "' element";
  }

  public String getDescription() {
    if (node instanceof Element) {
      return describe((Element) node);
    } else {
      Attribute attr = (Attribute) node;
      return "'" + attr.getName() + "' attribute of " + describe(attr.getParent());
    }
  }

  public int getStartLine() {
    return getStartLine(node);
  }

  private static int getStartLine(Object node) {
    if (node instanceof InheritingElement) {
      return ((InheritingElement) node).getStartLine();
    } else if (node instanceof Located) {
      return ((Located) node).getLine();
    } else if (node instanceof Attribute) {
      return getStartLine(((Attribute) node).getParent());
    } else {
      return 0;
    }
  }

  public int getEndLine() {
    return getEndLine(node);
  }

  public static int getEndLine(Object node) {
    if (node instanceof InheritingElement) {
      return ((InheritingElement) node).getEndLine();
    } else if (node instanceof Located) {
      return ((Located) node).getLine();
    } else if (node instanceof Attribute) {
      return getEndLine(((Attribute) node).getParent());
    } else {
      return 0;
    }
  }

  public int getColumn() {
    if (node instanceof Located) {
      return ((Located) node).getColumn();
    } else {
      return 0;
    }
  }

  private static Node wrapUnique(Node prev, boolean unique, String name, Object thing)
      throws InvalidXMLException {
    if (thing == null) return prev;
    Node node = thing instanceof Element ? new Node((Element) thing) : new Node((Attribute) thing);
    if (unique && prev != null)
      throw new InvalidXMLException("Multiple values for '" + name + "'", node);
    return node;
  }

  /**
   * Return a new Node wrapping an Attribute of the given Element matching one of the given names,
   * or null if the given Element has no matching Attributes.
   */
  public static @Nullable Node fromAttr(Element el, String... aliases) throws InvalidXMLException {
    Node node = null;
    for (String alias : aliases) {
      node = wrapUnique(node, true, alias, el.getAttribute(alias));
    }
    return node;
  }

  /**
   * Return a new Node wrapping an Attribute of the given Element matching one of the given names,
   * or the element itself if the given Element has no matching Attributes. Note: this is mostly
   * useful for properties that are allowed as either refs or direct children.
   */
  public static Node fromAttrOrSelf(Element el, String... aliases) throws InvalidXMLException {
    Node node = fromAttr(el, aliases);
    return node != null ? node : new Node(el);
  }

  /**
   * Return a new Node wrapping the named Attribute of the given Element. If the Attribute does not
   * exist, throw an InvalidXMLException complaining about it.
   */
  public static Node fromRequiredAttr(Element el, String... aliases) throws InvalidXMLException {
    Node node = fromAttr(el, aliases);
    if (node == null) {
      throw new InvalidXMLException("attribute '" + aliases[0] + "' is required", el);
    }
    return node;
  }

  public static List<Node> fromChildren(List<Node> nodes, Element el, String... aliases)
      throws InvalidXMLException {
    for (Element child : ((InheritingElement) el).getChildren(Sets.newHashSet(aliases))) {
      nodes.add(new Node(child));
    }
    return nodes;
  }

  public static List<Node> fromChildren(Element el, String... aliases) throws InvalidXMLException {
    return fromChildren(new ArrayList<>(), el, aliases);
  }

  public static @Nullable Node fromNullable(Element el) {
    return el == null ? null : new Node(el);
  }

  public static @Nullable Node fromNullable(Attribute attr) {
    return attr == null ? null : new Node(attr);
  }

  public static @Nullable Node fromChildOrAttr(Element el, boolean unique, String... aliases)
      throws InvalidXMLException {
    Node node = null;
    for (String alias : aliases) {
      node = wrapUnique(node, unique, alias, el.getAttribute(alias));
      for (Element child : el.getChildren(alias)) {
        node = wrapUnique(node, unique, alias, child);
      }
    }
    return node;
  }

  public static @Nullable Node fromChildOrAttr(Element el, String... aliases)
      throws InvalidXMLException {
    return fromChildOrAttr(el, true, aliases);
  }

  public static @Nullable Node fromLastChildOrAttr(Element el, String... aliases)
      throws InvalidXMLException {
    return fromChildOrAttr(el, false, aliases);
  }

  public static Node fromRequiredChildOrAttr(Element el, String... aliases)
      throws InvalidXMLException {
    Node node = fromChildOrAttr(el, aliases);
    if (node == null) {
      throw new InvalidXMLException(
          "attribute or child element '" + aliases[0] + "' is required", el);
    }
    return node;
  }

  public static @Nullable Node from(
      boolean attr, boolean child, boolean self, boolean required, Element el, String... aliases)
      throws InvalidXMLException {
    assertTrue(el != null || !required, "Cannot request required from null element");
    assertTrue(!child || !self, "Child and self are mutually exclusive");
    if (el == null) return null;
    Node node = null;

    if (attr) {
      if (child) node = Node.fromChildOrAttr(el, aliases);
      else if (self) node = Node.fromAttrOrSelf(el, aliases);
      else node = Node.fromAttr(el, aliases);
    } else {
      if (child) node = Node.fromNullable(XMLUtils.getUniqueChild(el, aliases));
      else if (self) node = Node.fromNullable(el);
    }

    if (required && node == null) {
      if (attr) {
        if (child)
          throw new InvalidXMLException(
              "attribute or child element '" + aliases[0] + "' is required", el);
        else throw new InvalidXMLException("attribute '" + aliases[0] + "' is required", el);
      } else {
        if (child)
          throw new InvalidXMLException("child element '" + aliases[0] + "' is required", el);
        else throw new IllegalStateException();
      }
    }
    return node;
  }
}
