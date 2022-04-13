package tc.oc.pgm.goals;

import java.util.HashSet;
import java.util.Set;
import org.jdom2.Element;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class ShowOptions {

  public enum ShowFlag {
    SHOW_MESSAGES("show-messages"),
    SHOW_EFFECTS("show-effects"),
    SHOW_INFO("show-info"),
    SHOW_SIDEBAR("show-sidebar"),
    STATS("stats");

    private final String name;

    ShowFlag(String name) {
      this.name = name;
    }
  }

  private final Set<ShowFlag> flags;

  private ShowOptions(Set<ShowFlag> flags) {
    this.flags = flags;
  }

  public static ShowOptions parse(Element el) throws InvalidXMLException {
    Set<ShowFlag> flags = new HashSet<>();
    boolean show = XMLUtils.parseBoolean(el.getAttribute("show"), true);
    for (ShowFlag showFlag : ShowFlag.values()) {
      if (XMLUtils.parseBoolean(el.getAttribute(showFlag.name), show)) {
        flags.add(showFlag);
      }
    }
    return new ShowOptions(flags);
  }

  public boolean hasFlag(ShowFlag flag) {
    return flags.contains(flag);
  }
}
