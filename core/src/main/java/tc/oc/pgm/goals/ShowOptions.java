package tc.oc.pgm.goals;

import java.util.EnumSet;
import java.util.Set;
import org.jdom2.Element;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class ShowOptions {

  private final Set<ShowOption> options;

  private ShowOptions(Set<ShowOption> options) {
    this.options = options;
  }

  public static ShowOptions parse(Element el) throws InvalidXMLException {
    Set<ShowOption> options = EnumSet.noneOf(ShowOption.class);
    boolean show = XMLUtils.parseBoolean(el.getAttribute("show"), true);
    for (ShowOption option : ShowOption.values()) {
      if (XMLUtils.parseBoolean(el.getAttribute(option.getName()), show)) {
        options.add(option);
      }
    }
    return new ShowOptions(options);
  }

  public boolean hasOption(ShowOption option) {
    return options.contains(option);
  }

  @Override
  public String toString() {
    return options.toString();
  }
}
