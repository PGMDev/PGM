package tc.oc.item;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Potion-related utilities. */
public interface Potions {

  static Collection<PotionEffect> getEffects(ItemStack potion) {
    if (potion.getItemMeta() instanceof PotionMeta) {
      PotionMeta meta = (PotionMeta) potion.getItemMeta();
      if (meta.hasCustomEffects()) {
        return meta.getCustomEffects();
      } else {
        return Potion.fromItemStack(potion).getEffects();
      }
    } else {
      return Collections.emptyList();
    }
  }

  static @Nullable PotionEffect getPrimaryEffect(ItemStack potion) {
    for (PotionEffect effect : Potions.getEffects(potion)) {
      return effect;
    }
    return null;
  }

  static @Nullable PotionEffectType getPrimaryEffectType(ItemStack potion) {
    for (PotionEffect effect : Potions.getEffects(potion)) {
      return effect.getType();
    }
    return null;
  }

  /** Potion effects mapped to their implications. */
  static final ImmutableMap<PotionEffectType, PotionClassification> potionEffectTypeImplications =
      ImmutableMap.<PotionEffectType, PotionClassification>builder()
          //  Harmful effects
          .put(PotionEffectType.BLINDNESS, PotionClassification.HARMFUL)
          .put(PotionEffectType.CONFUSION, PotionClassification.HARMFUL)
          .put(PotionEffectType.HARM, PotionClassification.HARMFUL)
          .put(PotionEffectType.HUNGER, PotionClassification.HARMFUL)
          .put(PotionEffectType.POISON, PotionClassification.HARMFUL)
          .put(PotionEffectType.SLOW, PotionClassification.HARMFUL)
          .put(PotionEffectType.SLOW_DIGGING, PotionClassification.HARMFUL)
          .put(PotionEffectType.WEAKNESS, PotionClassification.HARMFUL)
          .put(PotionEffectType.WITHER, PotionClassification.HARMFUL)
          //  Beneficial effects
          .put(PotionEffectType.FAST_DIGGING, PotionClassification.BENEFICIAL)
          .put(PotionEffectType.FIRE_RESISTANCE, PotionClassification.BENEFICIAL)
          .put(PotionEffectType.HEAL, PotionClassification.BENEFICIAL)
          .put(PotionEffectType.HEALTH_BOOST, PotionClassification.BENEFICIAL)
          .put(PotionEffectType.INCREASE_DAMAGE, PotionClassification.BENEFICIAL)
          .put(PotionEffectType.INVISIBILITY, PotionClassification.BENEFICIAL)
          .put(PotionEffectType.JUMP, PotionClassification.BENEFICIAL)
          .put(PotionEffectType.NIGHT_VISION, PotionClassification.BENEFICIAL)
          .put(PotionEffectType.REGENERATION, PotionClassification.BENEFICIAL)
          .put(PotionEffectType.SATURATION, PotionClassification.BENEFICIAL)
          .put(PotionEffectType.SPEED, PotionClassification.BENEFICIAL)
          .put(PotionEffectType.WATER_BREATHING, PotionClassification.BENEFICIAL)
          .build();
  /** Potion effects mapped to their negative (inverse) implications. */
  static final ImmutableMap<PotionEffectType, PotionClassification>
      inversePotionEffectTypeImplications =
          ImmutableMap.<PotionEffectType, PotionClassification>builder()
              //  SLOW
              .put(PotionEffectType.SPEED, PotionClassification.HARMFUL)
              //  SPEED
              .put(PotionEffectType.SLOW, PotionClassification.BENEFICIAL)
              //  SLOW_DIGGING
              .put(PotionEffectType.FAST_DIGGING, PotionClassification.HARMFUL)
              //  FAST_DIGGING
              .put(PotionEffectType.SLOW_DIGGING, PotionClassification.BENEFICIAL)
              //  WEAKNESS
              .put(PotionEffectType.INCREASE_DAMAGE, PotionClassification.HARMFUL)
              //  Normal behavior
              .put(PotionEffectType.HEAL, PotionClassification.BENEFICIAL)
              /*
              ~ Behaves very oddly:
              ~ Level < 0 && > -5: Constant damage sound/animation. Damage is actually being taken, but amount is negative,
                causing no loss in health. Upon taking any positive damage (regular damage) via other means, the player dies
                instantly.
              ~ Level <= -5: Instant death.
              */
              .put(PotionEffectType.HARM, PotionClassification.HARMFUL)
              /* Inverse of JUMP
              ~ Side effect: appears to increase fall damage sensitivity.
              */
              .put(PotionEffectType.JUMP, PotionClassification.HARMFUL)
              //  Normal behavior
              .put(PotionEffectType.CONFUSION, PotionClassification.HARMFUL)
              //  Normal behavior
              .put(PotionEffectType.REGENERATION, PotionClassification.BENEFICIAL)
              //  Inverse of DAMAGE_RESISTANCE
              .put(PotionEffectType.DAMAGE_RESISTANCE, PotionClassification.HARMFUL)
              //  Normal behavior
              .put(PotionEffectType.FIRE_RESISTANCE, PotionClassification.BENEFICIAL)
              //  Normal behavior
              .put(PotionEffectType.WATER_BREATHING, PotionClassification.BENEFICIAL)
              //  Normal behavior
              .put(PotionEffectType.INVISIBILITY, PotionClassification.BENEFICIAL)
              //  Normal behavior
              .put(PotionEffectType.BLINDNESS, PotionClassification.HARMFUL)
              //  Normal behavior
              .put(PotionEffectType.NIGHT_VISION, PotionClassification.BENEFICIAL)
              //  SATURATION
              .put(PotionEffectType.HUNGER, PotionClassification.BENEFICIAL)
              //  INCREASE_DAMAGE
              .put(PotionEffectType.WEAKNESS, PotionClassification.BENEFICIAL)
              //  Normal behavior
              .put(PotionEffectType.POISON, PotionClassification.HARMFUL)
              //  Normal behavior
              .put(PotionEffectType.WITHER, PotionClassification.HARMFUL)
              //  Inverse of HEALTH_BOOST. When all hearts are removed (<= -5), the player instantly
              // dies.
              .put(PotionEffectType.HEALTH_BOOST, PotionClassification.HARMFUL)
              //  Normal behavior
              .put(PotionEffectType.ABSORPTION, PotionClassification.BENEFICIAL)
              /* HUNGER
              ~ Side effect: adds 10 absorption hearts at the start of the effect.
              */
              .put(PotionEffectType.SATURATION, PotionClassification.HARMFUL)
              .build();

  /** Determines whether or not the specified {@link ThrownPotion} is harmful. */
  static boolean isHarmful(final ThrownPotion potion) {
    return getClassificationForPotion(potion).equals(PotionClassification.HARMFUL);
  }

  /** Determines whether or not the specified {@link PotionEffect}s--when combined--are harmful. */
  static boolean isHarmful(final Iterable<PotionEffect> effects) {
    return getClassificationForEffects(effects).equals(PotionClassification.HARMFUL);
  }

  /**
   * Scores the specified {@link ThrownPotion}.
   *
   * @param potion The potion to score.
   * @return The score.
   */
  static double getScore(final ThrownPotion potion) {
    Preconditions.checkNotNull(potion, "Potion");
    return getScore(
        Iterables.concat(
            ((PotionMeta) potion.getItem().getItemMeta()).getCustomEffects(), potion.getEffects()));
  }

  /**
   * Scores the specified {@link PotionEffect}s.
   *
   * @param effects The potion effects to score.
   * @return The score.
   */
  static double getScore(final Iterable<PotionEffect> effects) {
    Preconditions.checkNotNull(effects, "Effects");

    double score = 0;
    for (PotionEffect effect : effects) {
      try {
        score += getScore(effect);
      } catch (IllegalArgumentException ignored) {
      } //  Ignore unrecognized effects
    }

    return score;
  }

  /**
   * Scores the specified {@link PotionEffect}.
   *
   * @param effect The effect to score.
   * @return The score.
   * @throws IllegalArgumentException If an effect with an unknown {@link PotionEffectType} was
   *     specified.
   */
  static double getScore(final PotionEffect effect) throws IllegalArgumentException {
    Preconditions.checkNotNull(effect, "Effect");

    //  Duration, in ticks
    int duration = effect.getDuration();
    //  Level (>= 1 is normal effect, == 0 is no effect, < 0 is inverse effect)
    int level = effect.getAmplifier();
    PotionEffectType effectType = effect.getType();
    PotionClassification classification =
        (level >= 0
            ? Potions.potionEffectTypeImplications.get(effectType)
            : Potions.inversePotionEffectTypeImplications.get(effectType));

    if (effectType == null || classification == null)
      throw new IllegalArgumentException("Unknown (or null) PotionEffectType");

    double score = classification.getScore();
    score *= Math.abs(level);
    return score * ((double) duration / 20);
  }

  /**
   * Classifies the provided {@link ThrownPotion} as either {@link PotionClassification#BENEFICIAL},
   * {@link PotionClassification#UNKNOWN}, or {@link PotionClassification#HARMFUL}.
   */
  static PotionClassification getClassificationForPotion(final ThrownPotion potion) {
    return PotionClassification.getClassificationForScore(getScore(potion));
  }

  /**
   * Classifies the provided {@link PotionEffect}s as either {@link
   * PotionClassification#BENEFICIAL}, {@link PotionClassification#UNKNOWN}, or {@link
   * PotionClassification#HARMFUL}.
   */
  static PotionClassification getClassificationForEffects(final Iterable<PotionEffect> effects) {
    return PotionClassification.getClassificationForScore(getScore(effects));
  }

  /**
   * Classification for potions that represents their harmfulness (or lack thereof). Not explicitly
   * limited to {@link org.bukkit.entity.ThrownPotion}s; other logical use cases include
   * representation of {@link org.bukkit.potion.PotionEffect}s and {@link
   * org.bukkit.potion.PotionEffectType}s.
   */
  enum PotionClassification {
    /** Beneficial, has positive implications */
    BENEFICIAL(1),
    /** Unknown, positiveness of implications is not determinable */
    UNKNOWN(0),
    /** Harmful, has negative implications */
    HARMFUL(-1);
    private final int score;

    /** @param score Score of implications' positiveness */
    private PotionClassification(final int score) {
      this.score = score;
    }

    /**
     * Gets a classification for the given score. {@link #BENEFICIAL} if > 0, {@link #UNKNOWN} if ==
     * 0, {@link #HARMFUL} if < 0.
     */
    static PotionClassification getClassificationForScore(final double score) {
      return (score > 0.0 ? BENEFICIAL : (score == 0.0 ? UNKNOWN : HARMFUL));
    }

    /** An integer representation of this classification */
    public int getScore() {
      return this.score;
    }
  }

  static int getEffectLevel(LivingEntity entity, PotionEffectType type) {
    int level = 0;
    for (PotionEffect effect : entity.getActivePotionEffects()) {
      if (effect.getType() == type && effect.getAmplifier() + 1 > level) {
        level = effect.getAmplifier() + 1;
      }
    }
    return level;
  }
}
