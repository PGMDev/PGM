package tc.oc.pgm.variables;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.jdom2.Element;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.variables.types.LocalVariable;

public class LocalScopes {

  private final Deque<FunctionScope> functions = new ArrayDeque<>();

  public LocalFrame.Layout enterFunction(Element root) {
    var function = new FunctionScope(root);
    functions.push(function);
    return function.layout;
  }

  public void exitFunction() {
    functions.pop();
  }

  public boolean isInFunction(Element el) {
    var function = functions.peek();
    if (function == null) return false;
    for (var parent = el.getParentElement(); parent != null; parent = parent.getParentElement()) {
      if (parent == function.root) return true;
      if (FeatureDefinitionContext.parseId(parent) != null) return false;
    }
    return false;
  }

  public LocalVariable declare(Element el, String name) throws InvalidXMLException {
    var function = functions.peek();
    if (function == null || function.root == el)
      throw new InvalidXMLException("Local variables can only be declared inside actions", el);
    if (lookup(el, name) != null)
      throw new InvalidXMLException("Local variable '" + name + "' is already defined", el);

    var variable = new LocalVariable(name, function.layout, function.layout.allocate());
    function.declarations.add(new Declaration(el.getParentElement(), variable));
    return variable;
  }

  public @Nullable LocalVariable lookup(Node node, String name) {
    return lookup(elementOf(node), name);
  }

  private @Nullable LocalVariable lookup(Element el, String name) {
    return visible(el).get(name);
  }

  public Map<String, LocalVariable> visible(Node node) {
    return visible(elementOf(node));
  }

  private Map<String, LocalVariable> visible(Element el) {
    var function = current(el);
    if (function == null || function.declarations.isEmpty()) return Map.of();

    Map<String, LocalVariable> result = new HashMap<>();
    for (var declaration : function.declarations) {
      if (declaration.block.isAncestor(el))
        result.put(declaration.variable.getName(), declaration.variable);
    }
    return result;
  }

  public void markUsed(Node node, LocalVariable variable) {
    var function = functions.peek();
    if (function != null) function.usages.add(new Usage(elementOf(node), variable));
  }

  // Features with an id can be referenced from anywhere, so they cannot capture locals
  public void checkEscape(Element el, String id) throws InvalidXMLException {
    var function = functions.peek();
    if (function == null || !function.root.isAncestor(el)) return;
    for (var usage : function.usages) {
      if (usage.element == el || el.isAncestor(usage.element))
        throw new InvalidXMLException(
            "'" + id + "' cannot use local variable '" + usage.variable.getName()
                + "' because it has an id",
            usage.element);
    }
  }

  public void checkHidden(Node node, Predicate<String> isUsed) throws InvalidXMLException {
    Element el = elementOf(node);
    var inner = current(el);
    if (inner == null) return;
    for (var function : functions) {
      if (function == inner) continue;
      for (var declaration : function.declarations) {
        var name = declaration.variable.getName();
        if (lookup(el, name) != null) continue;
        if (declaration.block.isAncestor(el) && isUsed.test(name))
          throw new InvalidXMLException(
              "Local variable '" + name
                  + "' is declared outside of this feature, features with an id cannot use outer locals",
              node);
      }
    }
  }

  private @Nullable FunctionScope current(Element el) {
    var function = functions.peek();
    if (function == null) return null;
    return function.root == el || function.root.isAncestor(el) ? function : null;
  }

  private static Element elementOf(Node node) {
    return node.isAttribute() ? node.getAttribute().getParent() : node.getElement();
  }

  private record Declaration(Element block, LocalVariable variable) {}

  private record Usage(Element element, LocalVariable variable) {}

  private static final class FunctionScope {
    private final Element root;
    private final LocalFrame.Layout layout = new LocalFrame.Layout();
    private final List<Declaration> declarations = new ArrayList<>();
    private final List<Usage> usages = new ArrayList<>();

    private FunctionScope(Element root) {
      this.root = root;
    }
  }
}
