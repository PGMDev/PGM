package tc.oc.pgm.util.xml.parsers;

import org.jdom2.Element;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class BoolBuilder extends PrimitiveBuilder<Boolean, BoolBuilder> {
  public BoolBuilder(Element el, String... prop) {
    super(el, prop);
  }

  public boolean orTrue() throws InvalidXMLException {
    return optional(true);
  }

  public boolean orFalse() throws InvalidXMLException {
    return optional(false);
  }

  @Override
  protected Boolean parse(String text) throws TextException {
    return TextParser.parseBoolean(text);
  }

  @Override
  protected BoolBuilder getThis() {
    return this;
  }
}
