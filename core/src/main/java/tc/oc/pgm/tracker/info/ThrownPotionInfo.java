package tc.oc.pgm.tracker.info;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.api.tracker.info.PotionInfo;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.text.MinecraftComponent;

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
  public @Nullable PhysicalInfo damager() {
    return this;
  }

  @Override
  public @Nullable ParticipantState attacker() {
    return owner();
  }

  @Override
  public @Nullable PotionEffectType potionEffect() {
    return effectType;
  }

  @Override
  public Component name() {
    return MinecraftComponent.potion(potionEffect());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{type=" + entityType() + " potion=" + potionEffect() + "}";
  }
}
