package tc.oc.pgm.util.xml.parsers;

import org.bukkit.ChatColor;
import org.jdom2.Element;
import tc.oc.pgm.util.text.TextException;

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
    if (colored) text = ChatColor.translateAlternateColorCodes('`', text);
    return text;
  }

  @Override
  protected StringBuilder getThis() {
    return this;
  }
}
