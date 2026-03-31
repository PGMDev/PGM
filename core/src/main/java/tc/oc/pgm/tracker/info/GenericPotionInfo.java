package tc.oc.pgm.tracker.info;

import net.kyori.adventure.text.Component;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.PotionInfo;
import tc.oc.pgm.util.text.MinecraftComponent;

public record GenericPotionInfo(@Nullable PotionEffectType potionEffect) implements PotionInfo {

  @Override
  public String identifier() {
    PotionEffectType effectType = potionEffect();
    return effectType != null ? effectType.getName() : "EMPTY";
  }

  @Override
  public Component name() {
    return MinecraftComponent.potion(potionEffect());
  }

  @Override
  public @Nullable ParticipantState owner() {
    return null;
  }

  @Override
  public @Nullable ParticipantState attacker() {
    return null;
  }

  @Override
  public @NonNull String toString() {
    return getClass().getSimpleName() + "{potion=" + potionEffect() + "}";
  }
}
