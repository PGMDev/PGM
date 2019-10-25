package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.match.ParticipantState;
import tc.oc.world.NMSHacks;

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
  public @Nullable ParticipantState getAttacker() {
    return getOwner();
  }

  public Material getMaterial() {
    return material;
  }

  @Override
  public Component getLocalizedName() {
    return new PersonalizedTranslatable(NMSHacks.getTranslationKey(getMaterial()));
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
