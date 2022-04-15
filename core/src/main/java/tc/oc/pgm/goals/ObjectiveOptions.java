package tc.oc.pgm.goals;

import java.util.EnumSet;
import java.util.Set;
import org.jdom2.Element;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class ObjectiveOptions {

  private final Set<ObjectiveOption> options;

  private ObjectiveOptions(Set<ObjectiveOption> options) {
    this.options = options;
  }

  public static ObjectiveOptions parse(Element el) throws InvalidXMLException {
    Set<ObjectiveOption> options = EnumSet.noneOf(ObjectiveOption.class);
    boolean show = XMLUtils.parseBoolean(el.getAttribute("show"), true);
    for (ObjectiveOption option : ObjectiveOption.values()) {
      if (XMLUtils.parseBoolean(el.getAttribute(option.getName()), show)) {
        options.add(option);
      }
    }
    return new ObjectiveOptions(options);
  }

  public boolean hasOption(ObjectiveOption option) {
    return options.contains(option);
  }

  @Override
  public String toString() {
    return options.toString();
  }
}
