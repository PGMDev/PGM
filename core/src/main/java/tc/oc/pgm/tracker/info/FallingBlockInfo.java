package tc.oc.pgm.tracker.info;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.util.text.MinecraftComponent;

@Getter
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
  public @Nullable PhysicalInfo getDamager() {
    return this;
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return getOwner();
  }

  @Override
  public Component getName() {
    return MinecraftComponent.material(getMaterial());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{world="
        + getMaterial()
        + " name="
        + getCustomName()
        + " owner="
        + getOwner()
        + "}";
  }
}
