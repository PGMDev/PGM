package tc.oc.pgm.util.attribute;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AttributeInstance {

  private final net.minecraft.server.v1_8_R3.AttributeInstance handle;
  private final Attribute attribute;

  public AttributeInstance(
      net.minecraft.server.v1_8_R3.AttributeInstance handle, Attribute attribute) {
    this.handle = handle;
    this.attribute = attribute;
  }

  public Attribute getAttribute() {
    return attribute;
  }

  public double getBaseValue() {
    return handle.b();
  }

  public void setBaseValue(double d) {
    handle.setValue(d);
  }

  public Collection<AttributeModifier> getModifiers() {
    List<AttributeModifier> result = new ArrayList<AttributeModifier>();
    for (net.minecraft.server.v1_8_R3.AttributeModifier nms : handle.c()) {
      result.add(convert(nms));
    }

    return result;
  }

  public void addModifier(AttributeModifier modifier) {
    Preconditions.checkArgument(modifier != null, "modifier");
    handle.b(convert(modifier));
  }

  public void removeModifier(AttributeModifier modifier) {
    Preconditions.checkArgument(modifier != null, "modifier");
    handle.c(convert(modifier));
  }

  public double getValue() {
    return handle.getValue();
  }

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
