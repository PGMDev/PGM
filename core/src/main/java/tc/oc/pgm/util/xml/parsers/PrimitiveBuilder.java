package tc.oc.pgm.util.xml.parsers;

import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public abstract class PrimitiveBuilder<T, B extends PrimitiveBuilder<T, B>> extends Builder<T, B> {

  public PrimitiveBuilder(Element el, String... prop) {
    super(el, prop);
  }

  @Override
  protected T parse(Node node) throws InvalidXMLException {
    try {
      return parse(node.getValueNormalize());
    } catch (TextException e) {
      throw new InvalidXMLException(node, e);
    }
  }

  protected abstract T parse(String text) throws TextException;

  public abstract static class Generic<T> extends PrimitiveBuilder<T, PrimitiveBuilder.Generic<T>> {
    public Generic(@Nullable Element el, String... prop) {
      super(el, prop);
    }

    @Override
    protected PrimitiveBuilder.Generic<T> getThis() {
      return this;
    }
  }
}
