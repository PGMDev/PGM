package tc.oc.pgm.util.inventory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Map;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.util.bukkit.PotionEffects;

public final class PotionClassifier {

  private static final int HARMFUL = -1;
  private static final int UNKNOWN = 0;
  private static final int BENEFICIAL = 1;

  /** Potion effects mapped to their implications. */
  private static final Map<PotionEffectType, Integer> potionEffectTypeImplications =
      ImmutableMap.<PotionEffectType, Integer>builder()
          //  Harmful effects
          .put(PotionEffectType.BLINDNESS, HARMFUL)
          .put(PotionEffects.NAUSEA, HARMFUL)
          .put(PotionEffects.INSTANT_DAMAGE, HARMFUL)
          .put(PotionEffectType.HUNGER, HARMFUL)
          .put(PotionEffectType.POISON, HARMFUL)
          .put(PotionEffects.SLOWNESS, HARMFUL)
          .put(PotionEffects.MINING_FATIGUE, HARMFUL)
          .put(PotionEffectType.WEAKNESS, HARMFUL)
          .put(PotionEffectType.WITHER, HARMFUL)
          //  Beneficial effects
          .put(PotionEffects.HASTE, BENEFICIAL)
          .put(PotionEffectType.FIRE_RESISTANCE, BENEFICIAL)
          .put(PotionEffects.INSTANT_HEALTH, BENEFICIAL)
          .put(PotionEffectType.HEALTH_BOOST, BENEFICIAL)
          .put(PotionEffects.STRENGTH, BENEFICIAL)
          .put(PotionEffectType.INVISIBILITY, BENEFICIAL)
          .put(PotionEffects.JUMP_BOOST, BENEFICIAL)
          .put(PotionEffectType.NIGHT_VISION, BENEFICIAL)
          .put(PotionEffectType.REGENERATION, BENEFICIAL)
          .put(PotionEffectType.SATURATION, BENEFICIAL)
          .put(PotionEffectType.SPEED, BENEFICIAL)
          .put(PotionEffectType.WATER_BREATHING, BENEFICIAL)
          .build();
  /** Potion effects mapped to their negative (inverse) implications. */
  private static final Map<PotionEffectType, Integer> inversePotionEffectTypeImplications =
      ImmutableMap.<PotionEffectType, Integer>builder()
          //  SLOW
          .put(PotionEffectType.SPEED, HARMFUL)
          //  SPEED
          .put(PotionEffects.SLOWNESS, BENEFICIAL)
          //  MINING_FATIGUE
          .put(PotionEffects.HASTE, HARMFUL)
          //  HASTE
          .put(PotionEffects.MINING_FATIGUE, BENEFICIAL)
          //  WEAKNESS
          .put(PotionEffects.STRENGTH, HARMFUL)
          //  Normal behavior
          .put(PotionEffects.INSTANT_HEALTH, BENEFICIAL)
          /*
          ~ Behaves very oddly:
          ~ Level < 0 && > -5: Constant damage sound/animation. Damage is actually being taken, but amount is negative,
            causing no loss in health. Upon taking any positive damage (regular damage) via other means, the player dies
            instantly.
          ~ Level <= -5: Instant death.
          */
          .put(PotionEffects.INSTANT_DAMAGE, HARMFUL)
          /* Inverse of JUMP
          ~ Side effect: appears to increase fall damage sensitivity.
          */
          .put(PotionEffects.JUMP_BOOST, HARMFUL)
          //  Normal behavior
          .put(PotionEffects.NAUSEA, HARMFUL)
          //  Normal behavior
          .put(PotionEffectType.REGENERATION, BENEFICIAL)
          //  Inverse of DAMAGE_RESISTANCE
          .put(PotionEffects.RESISTANCE, HARMFUL)
          //  Normal behavior
          .put(PotionEffectType.FIRE_RESISTANCE, BENEFICIAL)
          //  Normal behavior
          .put(PotionEffectType.WATER_BREATHING, BENEFICIAL)
          //  Normal behavior
          .put(PotionEffectType.INVISIBILITY, BENEFICIAL)
          //  Normal behavior
          .put(PotionEffectType.BLINDNESS, HARMFUL)
          //  Normal behavior
          .put(PotionEffectType.NIGHT_VISION, BENEFICIAL)
          //  SATURATION
          .put(PotionEffectType.HUNGER, BENEFICIAL)
          //  INCREASE_DAMAGE
          .put(PotionEffectType.WEAKNESS, BENEFICIAL)
          //  Normal behavior
          .put(PotionEffectType.POISON, HARMFUL)
          //  Normal behavior
          .put(PotionEffectType.WITHER, HARMFUL)
          //  Inverse of HEALTH_BOOST. When all hearts are removed (<= -5), the player instantly
          // dies.
          .put(PotionEffectType.HEALTH_BOOST, HARMFUL)
          //  Normal behavior
          .put(PotionEffectType.ABSORPTION, BENEFICIAL)
          /* HUNGER
          ~ Side effect: adds 10 absorption hearts at the start of the effect.
          */
          .put(PotionEffectType.SATURATION, HARMFUL)
          .build();

  public static double getScore(ThrownPotion potion) {
    double score = 0;

    for (PotionEffect effect : Iterables.concat(
        potion.getEffects(), ((PotionMeta) potion.getItem().getItemMeta()).getCustomEffects())) {
      score += getScore(effect);
    }

    return score;
  }

  public static double getScore(final PotionEffect effect) throws IllegalArgumentException {
    int level =
        effect.getAmplifier(); //  Level (>= 1 is normal effect, == 0 is no effect, < 0 is inverse
    // effect)
    PotionEffectType effectType = effect.getType();

    return (level >= 0
            ? potionEffectTypeImplications.getOrDefault(effectType, UNKNOWN)
            : inversePotionEffectTypeImplications.getOrDefault(effectType, UNKNOWN))
        * Math.abs(level)
        * ((double) effect.getDuration() / 20);
  }

  public static boolean isHarmful(ThrownPotion potion) {
    return getScore(potion) <= HARMFUL;
  }
}
