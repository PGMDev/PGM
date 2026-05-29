package tc.oc.pgm.util.xml.parsers;

import java.util.regex.Pattern;
import org.jdom2.Element;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class StringBuilder extends PrimitiveBuilder<String, StringBuilder> {
  private boolean colored;

  public StringBuilder(Element el, String... props) {
    super(el, props);
  }

  public StringBuilder colored() {
    this.colored = true;
    return this;
  }

  @Override
  protected String parse(String text) throws TextException {
    if (colored) text = BukkitUtils.colorize(text);
    return text;
  }

  public StringBuilder validate(Pattern pattern) {
    var predicate = pattern.asMatchPredicate();
    return this.validate((s, node) -> {
      if (!predicate.test(s))
        throw new InvalidXMLException("Expected string to match " + pattern, el);
    });
  }

  @Override
  protected StringBuilder getThis() {
    return this;
  }
}
