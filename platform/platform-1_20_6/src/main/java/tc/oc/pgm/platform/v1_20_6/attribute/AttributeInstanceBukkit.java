package tc.oc.pgm.platform.v1_20_6.attribute;

import java.util.ArrayList;
import java.util.Collection;
import tc.oc.pgm.util.attribute.Attribute;
import tc.oc.pgm.util.attribute.AttributeInstance;
import tc.oc.pgm.util.attribute.AttributeModifier;

/** Attribute Instance for 1.9+ */
public class AttributeInstanceBukkit implements AttributeInstance {

  private final org.bukkit.attribute.AttributeInstance bukkitAttributeInstance;

  public AttributeInstanceBukkit(org.bukkit.attribute.AttributeInstance bukkitAttributeInstance) {
    this.bukkitAttributeInstance = bukkitAttributeInstance;
  }

  @Override
  public Attribute getAttribute() {
    return AttributeUtilBukkit.convertAttribute(bukkitAttributeInstance.getAttribute());
  }

  @Override
  public double getBaseValue() {
    return bukkitAttributeInstance.getBaseValue();
  }

  @Override
  public void setBaseValue(double d) {
    bukkitAttributeInstance.setBaseValue(d);
  }

  @Override
  public Collection<AttributeModifier> getModifiers() {
    Collection<AttributeModifier> convertedModifiers = new ArrayList<>();

    for (org.bukkit.attribute.AttributeModifier modifier : bukkitAttributeInstance.getModifiers()) {

      AttributeModifier attributeModifier = AttributeUtilBukkit.convertAttributeModifier(modifier);

      convertedModifiers.add(attributeModifier);
    }

    return convertedModifiers;
  }

  @Override
  public void addModifier(AttributeModifier modifier) {
    bukkitAttributeInstance.addModifier(AttributeUtilBukkit.convertAttributeModifier(modifier));
  }

  @Override
  public void removeModifier(AttributeModifier modifier) {
    bukkitAttributeInstance.removeModifier(AttributeUtilBukkit.convertAttributeModifier(modifier));
  }

  @Override
  public double getValue() {
    return bukkitAttributeInstance.getValue();
  }

  @Override
  public double getDefaultValue() {
    return bukkitAttributeInstance.getDefaultValue();
  }
}
