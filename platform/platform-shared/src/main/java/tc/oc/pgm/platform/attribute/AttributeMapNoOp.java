package tc.oc.pgm.platform.attribute;

import tc.oc.pgm.util.attribute.Attribute;
import tc.oc.pgm.util.attribute.AttributeInstance;
import tc.oc.pgm.util.attribute.AttributeMap;

public class AttributeMapNoOp implements AttributeMap {
  @Override
  public AttributeInstance getAttribute(Attribute attribute) {
    return null;
  }
}
