package tc.oc.pgm.damage;

import java.lang.reflect.Method;
import java.util.Map;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jdom2.Element;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class DamageParser {
  protected static final Map<String, Method> methodParsers =
      MethodParsers.getMethodParsersForClass(DamageParser.class);

  public DamageParser() {}

  public DamageCause parseDamageCause(Element el) throws InvalidXMLException {
    DamageCause cause = DamageCause.valueOf(el.getTextNormalize().toUpperCase().replace(" ", "_"));

    if (cause == null) {
      throw new InvalidXMLException("Invalid damage cause '" + el.getValue() + "'.", el);
    }
    return cause;
  }
}
