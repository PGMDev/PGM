package tc.oc.pgm.api.attribute;

import java.util.Collection;

public interface AttributeInstance {
  Attribute getAttribute();

  double getBaseValue();

  void setBaseValue(double d);

  Collection<AttributeModifier> getModifiers();

  void addModifier(AttributeModifier modifier);

  void removeModifier(AttributeModifier modifier);

  double getValue();

  double getDefaultValue();
}
