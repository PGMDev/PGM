package tc.oc.pgm.tracker.info;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.util.text.MinecraftComponent;

public class FallingBlockInfo extends EntityInfo implements DamageInfo {

  private final Material material;

  public FallingBlockInfo(FallingBlock entity, @Nullable ParticipantState owner) {
    super(entity, owner);
    this.material = entity.getMaterial();
  }

  public FallingBlockInfo(FallingBlock entity) {
    this(entity, null);
  }

  @Override
  public @Nullable PhysicalInfo damager() {
    return this;
  }

  @Override
  public @Nullable ParticipantState attacker() {
    return owner();
  }

  public Material material() {
    return material;
  }

  @Deprecated
  public Material getMaterial() {
    return material();
  }

  @Override
  public Component name() {
    return MinecraftComponent.material(material());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{world="
        + material()
        + " name="
        + customName()
        + " owner="
        + owner()
        + "}";
  }
}
