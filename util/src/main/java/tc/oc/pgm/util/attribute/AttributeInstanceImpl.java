package tc.oc.pgm.util.attribute;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import tc.oc.pgm.api.attribute.Attribute;
import tc.oc.pgm.api.attribute.AttributeInstance;
import tc.oc.pgm.api.attribute.AttributeModifier;

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
    List<AttributeModifier> result = new ArrayList<>();
    for (net.minecraft.server.v1_8_R3.AttributeModifier nms : handle.c()) {
      result.add(convert(nms));
    }

    return result;
  }

  @Override
  public void addModifier(AttributeModifier modifier) {
    Preconditions.checkArgument(modifier != null, "modifier");
    handle.b(convert(modifier));
  }

  @Override
  public void removeModifier(AttributeModifier modifier) {
    Preconditions.checkArgument(modifier != null, "modifier");
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

  public static AttributeModifierImpl convert(net.minecraft.server.v1_8_R3.AttributeModifier nms) {
    return new AttributeModifierImpl(
        nms.a(), nms.b(), nms.d(), AttributeModifierImpl.Operation.values()[nms.c()]);
  }
}
