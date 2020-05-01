package tc.oc.pgm.tracker.info;

import javax.annotation.Nullable;
import net.kyori.text.Component;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.PotionInfo;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.text.MinecraftTranslations;

public class ThrownPotionInfo extends EntityInfo implements PotionInfo {

  private final PotionEffectType effectType;

  public ThrownPotionInfo(ThrownPotion entity, @Nullable ParticipantState owner) {
    super(entity, owner);
    this.effectType = InventoryUtils.getPrimaryEffectType(entity.getItem());
  }

  public ThrownPotionInfo(ThrownPotion entity) {
    this(entity, null);
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return getOwner();
  }

  @Override
  public @Nullable PotionEffectType getPotionEffect() {
    return effectType;
  }

  @Override
  public Component getName() {
    return MinecraftTranslations.getPotion(getPotionEffect());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{type="
        + getEntityType()
        + " potion="
        + getPotionEffect()
        + "}";
  }
}
