package tc.oc.pgm.goals;

import java.util.EnumSet;
import java.util.Set;
import org.jdom2.Element;
import tc.oc.pgm.api.goal.ShowOption;
import tc.oc.pgm.api.goal.ShowOptions;
import tc.oc.pgm.api.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class ShowOptionsImpl implements ShowOptions {

  private final Set<ShowOption> options;

  private ShowOptionsImpl(Set<ShowOption> options) {
    this.options = options;
  }

  public static ShowOptionsImpl parse(Element el) throws InvalidXMLException {
    Set<ShowOption> options = EnumSet.noneOf(ShowOption.class);
    boolean show = XMLUtils.parseBoolean(el.getAttribute("show"), true);
    for (ShowOption option : ShowOption.values()) {
      if (XMLUtils.parseBoolean(el.getAttribute(option.getName()), show)) {
        options.add(option);
      }
    }
    return new ShowOptionsImpl(options);
  }

  @Override
  public boolean hasOption(ShowOption option) {
    return options.contains(option);
  }

  @Override
  public String toString() {
    return options.toString();
  }
}
