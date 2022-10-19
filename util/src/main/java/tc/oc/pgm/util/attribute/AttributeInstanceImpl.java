package tc.oc.pgm.util.attribute;

import static tc.oc.pgm.util.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AttributeInstanceImpl implements AttributeInstance {

  private final net.minecraft.server.v1_8_R3.AttributeInstance handle;
  private final Attribute attribute;

  public AttributeInstanceImpl(
      net.minecraft.server.v1_8_R3.AttributeInstance handle, Attribute attribute) {
    this.handle = handle;
    this.attribute = attribute;
  }

  @Override
  public Attribute getAttribute() {
    return attribute;
  }

  @Override
  public double getBaseValue() {
    return handle.b();
  }

  @Override
  public void setBaseValue(double d) {
    handle.setValue(d);
  }

  @Override
  public Collection<AttributeModifier> getModifiers() {
    List<AttributeModifier> result = new ArrayList<AttributeModifier>();
    for (net.minecraft.server.v1_8_R3.AttributeModifier nms : handle.c()) {
      result.add(convert(nms));
    }

    return result;
  }

  @Override
  public void addModifier(AttributeModifier modifier) {
    assertTrue(modifier != null, "modifier");
    handle.b(convert(modifier));
  }

  @Override
  public void removeModifier(AttributeModifier modifier) {
    assertTrue(modifier != null, "modifier");
    handle.c(convert(modifier));
  }

  @Override
  public double getValue() {
    return handle.getValue();
  }

  @Override
  public double getDefaultValue() {
    return handle.getAttribute().b();
  }

  public static net.minecraft.server.v1_8_R3.AttributeModifier convert(AttributeModifier bukkit) {
    return new net.minecraft.server.v1_8_R3.AttributeModifier(
        bukkit.getUniqueId(),
        bukkit.getName(),
        bukkit.getAmount(),
        bukkit.getOperation().ordinal());
  }

  public static AttributeModifier convert(net.minecraft.server.v1_8_R3.AttributeModifier nms) {
    return new AttributeModifier(
        nms.a(), nms.b(), nms.d(), AttributeModifier.Operation.values()[nms.c()]);
  }
}
