package tc.oc.pgm.util.bukkit;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.util.platform.Platform;

/**
 * Classification for potions that represents their harmfulness (or lack thereof). Not explicitly
 * limited to {@link org.bukkit.entity.ThrownPotion}s; other logical use cases include
 * representation of {@link org.bukkit.potion.PotionEffect}s and
 * {@link org.bukkit.potion.PotionEffectType}s.
 */
@NullMarked
public enum PotionClassification {
  /** Beneficial, has positive implications */
  BENEFICIAL(1),
  /** Unknown, positiveness of implications is not determinable */
  UNKNOWN(0),
  /** Harmful, has negative implications */
  HARMFUL(-1);

  private final int score;

  /** @param score Score of implications' positiveness */
  PotionClassification(int score) {
    this.score = score;
  }

  /** An integer representation of this classification */
  public int getScore() {
    return score;
  }

  public boolean isHarmful() {
    return this == HARMFUL;
  }

  public PotionClassification inverse() {
    return switch (this) {
      case BENEFICIAL -> HARMFUL;
      case HARMFUL -> BENEFICIAL;
      default -> this;
    };
  }

  public abstract static class PotionRegistry {
    protected static final Set<PotionEffectType> BENEFICIAL =
        ImmutableSet.<PotionEffectType>builder()
            .add(PotionEffects.ABSORPTION)
            .add(PotionEffects.FIRE_RESISTANCE)
            .add(PotionEffects.HASTE)
            .add(PotionEffects.HEALTH_BOOST)
            .add(PotionEffects.INSTANT_HEALTH)
            .add(PotionEffects.INVISIBILITY)
            .add(PotionEffects.JUMP_BOOST)
            .add(PotionEffects.NIGHT_VISION)
            .add(PotionEffects.REGENERATION)
            .add(PotionEffects.RESISTANCE)
            .add(PotionEffects.SATURATION)
            .add(PotionEffects.SPEED)
            .add(PotionEffects.STRENGTH)
            .add(PotionEffects.WATER_BREATHING)
            .build();

    protected static final Set<PotionEffectType> HARMFUL = ImmutableSet.<PotionEffectType>builder()
        .add(PotionEffects.BLINDNESS)
        .add(PotionEffects.HUNGER)
        .add(PotionEffects.INSTANT_DAMAGE)
        .add(PotionEffects.MINING_FATIGUE)
        .add(PotionEffects.NAUSEA)
        .add(PotionEffects.POISON)
        .add(PotionEffects.SLOWNESS)
        .add(PotionEffects.WEAKNESS)
        .add(PotionEffects.WITHER)
        .build();

    public Set<PotionEffectType> getBeneficial() {
      return BENEFICIAL;
    }

    public Set<PotionEffectType> getHarmful() {
      return HARMFUL;
    }
  }

  private static final Map<PotionEffectType, PotionClassification> CLASSIFICATIONS;

  static {
    PotionRegistry registry = Platform.get(PotionRegistry.class);
    Map<PotionEffectType, PotionClassification> classifications = new HashMap<>();
    for (PotionEffectType effectType : registry.getBeneficial())
      classifications.put(effectType, BENEFICIAL);
    for (PotionEffectType effectType : registry.getHarmful())
      classifications.put(effectType, HARMFUL);
    CLASSIFICATIONS = ImmutableMap.copyOf(classifications);
  }

  /**
   * Scores the specified {@link PotionEffect}.
   *
   * @param effect The effect to score.
   * @return The score.
   */
  private static double getScore(PotionEffect effect) throws IllegalArgumentException {
    int amplifier = effect.getAmplifier();
    return classify(effect.getType(), effect.getAmplifier()).getScore()
        * (amplifier < 0 ? -amplifier : amplifier + 1)
        * ((double) effect.getDuration() / 20);
  }

  /**
   * Scores the specified {@link PotionEffect}s.
   *
   * @param effects The potion effects to score.
   * @return The score.
   */
  private static double getScore(Iterable<PotionEffect> effects) {
    double score = 0;
    for (PotionEffect effect : effects) {
      score += getScore(effect);
    }
    return score;
  }

  /**
   * Gets a classification for the given score. {@link #BENEFICIAL} if > 0, {@link #UNKNOWN} if ==
   * 0, {@link #HARMFUL} if < 0.
   */
  private static PotionClassification fromScore(double score) {
    return score > 0 ? BENEFICIAL : score == 0 ? UNKNOWN : HARMFUL;
  }

  public static PotionClassification classify(PotionEffectType effectType) {
    return CLASSIFICATIONS.getOrDefault(effectType, UNKNOWN);
  }

  public static PotionClassification classify(PotionEffectType effectType, int amplifier) {
    PotionClassification classifier = classify(effectType);
    return amplifier < 0 ? classifier.inverse() : classifier;
  }

  public static boolean isHarmful(Iterable<PotionEffect> effects) {
    double score = getScore(effects);
    return fromScore(score).isHarmful();
  }

  public static boolean isHarmful(ThrownPotion potion) {
    // Avoid duplicates on modern while still checking custom NBT effects on SportPaper
    Map<PotionEffectType, PotionEffect> effects = new HashMap<>();
    for (PotionEffect effect : potion.getEffects()) {
      effects.putIfAbsent(effect.getType(), effect);
    }

    if (potion.getItem().getItemMeta() instanceof PotionMeta meta) {
      for (PotionEffect effect : meta.getCustomEffects()) {
        effects.putIfAbsent(effect.getType(), effect);
      }
    }

    return isHarmful(effects.values());
  }
}
