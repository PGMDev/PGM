package tc.oc.pgm.tracker.info;

import net.kyori.adventure.text.Component;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.PotionInfo;
import tc.oc.pgm.util.text.MinecraftComponent;

public class GenericPotionInfo implements PotionInfo {

  private final PotionEffectType effectType;

  public GenericPotionInfo(PotionEffectType effectType) {
    this.effectType = effectType;
  }

  @Override
  public @Nullable PotionEffectType getPotionEffect() {
    return effectType;
  }

  @Override
  public String getIdentifier() {
    PotionEffectType effectType = getPotionEffect();
    return effectType != null ? effectType.getName() : "EMPTY";
  }

  @Override
  public Component getName() {
    return MinecraftComponent.potion(getPotionEffect());
  }

  @Override
  public @Nullable ParticipantState getOwner() {
    return null;
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{potion=" + getPotionEffect() + "}";
  }
}
