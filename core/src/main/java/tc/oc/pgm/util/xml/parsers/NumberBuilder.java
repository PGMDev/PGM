package tc.oc.pgm.util.xml.parsers;

import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class NumberBuilder<T extends Number> extends Builder<T, NumberBuilder<T>> {

  private final Class<T> type;
  private boolean infinity;

  public NumberBuilder(Class<T> type, @Nullable Element el, String... prop) {
    super(el, prop);
    this.type = type;
  }

  /** Allow infinity like oo or -oo */
  public NumberBuilder<T> inf() {
    this.infinity = true;
    return this;
  }

  @Override
  protected T parse(Node node) throws InvalidXMLException {
    return XMLUtils.parseNumber(node, type, infinity);
  }

  @Override
  protected NumberBuilder<T> getThis() {
    return this;
  }
}
