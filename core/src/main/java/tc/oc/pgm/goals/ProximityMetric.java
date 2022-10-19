package tc.oc.pgm.goals;

import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ProximityMetric {
  public static enum Type {
    CLOSEST_PLAYER("closest player"),
    CLOSEST_BLOCK("closest block"),
    CLOSEST_KILL("closest kill"),
    NONE("no proximity");

    public final String description;

    Type(String description) {
      this.description = description;
    }
  }

  public final Type type;
  public final boolean horizontal;

  public ProximityMetric(Type type, boolean horizontal) {
    this.type = type;
    this.horizontal = horizontal;
  }

  public String name() {
    if (this.horizontal) {
      return this.type.name() + "_HORIZONTAL";
    } else {
      return this.type.name();
    }
  }

  public String description() {
    if (this.horizontal) {
      return this.type.description + " (horizontal)";
    } else {
      return this.type.description;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ProximityMetric)) return false;
    ProximityMetric that = (ProximityMetric) o;
    return this.type == that.type && this.horizontal == that.horizontal;
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + (horizontal ? 1 : 0);
    return result;
  }

  public static @Nullable ProximityMetric parse(Element el, ProximityMetric def)
      throws InvalidXMLException {
    return parse(el, "", def);
  }

  public static @Nullable ProximityMetric parse(Element el, String prefix, ProximityMetric def)
      throws InvalidXMLException {
    if (!prefix.isEmpty()) prefix = prefix + "-";

    ProximityMetric.Type type =
        XMLUtils.parseEnum(
            Node.fromAttr(el, prefix + "proximity-metric"),
            ProximityMetric.Type.class,
            "proximity metric",
            def.type);

    // If proximity metric is none, use null proximity so that it doesn't try to get tracked nor
    // shows in the scoreboard
    if (type == Type.NONE) return null;

    return new ProximityMetric(
        type,
        XMLUtils.parseBoolean(el.getAttribute(prefix + "proximity-horizontal"), def.horizontal));
  }
}
