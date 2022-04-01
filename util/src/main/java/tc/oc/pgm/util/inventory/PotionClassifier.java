package tc.oc.pgm.util.inventory;

import com.google.common.collect.Iterables;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

public final class PotionClassifier {
  public static double getScore(ThrownPotion potion) {
    double score = 0;

    for (PotionEffect effect :
        Iterables.concat(
            potion.getEffects(),
            ((PotionMeta) potion.getItem().getItemMeta()).getCustomEffects())) {
      score += getScore(effect);
    }

    return score;
  }

  public static double getScore(final PotionEffect effect) throws IllegalArgumentException {
    int level =
        effect.getAmplifier(); //  Level (>= 1 is normal effect, == 0 is no effect, < 0 is inverse
    // effect)

    return (level >= 0 ? scoreEffect(effect) : scoreEffect(effect) * -1)
        * Math.abs(level)
        * ((double) effect.getDuration() / 20);
  }

  public static boolean isHarmful(ThrownPotion potion) {
    return getScore(potion) <= -1;
  }

  public static int scoreEffect(PotionEffect effect) {
    switch (effect.getType().getEffectCategory()) {
      case BENEFICIAL:
        return 1;
      case HARMFUL:
        return -1;
      default:
        return 0;
    }
  }
}
