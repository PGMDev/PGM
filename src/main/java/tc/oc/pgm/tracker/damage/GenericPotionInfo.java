package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;
import org.bukkit.potion.PotionEffectType;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.match.ParticipantState;
import tc.oc.world.NMSHacks;

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
  public Component getLocalizedName() {
    return new PersonalizedTranslatable(NMSHacks.getTranslationKey(getPotionEffect()));
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
