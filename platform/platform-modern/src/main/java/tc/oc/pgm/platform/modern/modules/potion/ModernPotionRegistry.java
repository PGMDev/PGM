package tc.oc.pgm.platform.modern.modules.potion;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.util.bukkit.PotionClassification;
import tc.oc.pgm.util.platform.Supports;

@NullMarked
@Supports(PAPER)
public class ModernPotionRegistry extends PotionClassification.PotionRegistry {

  @Override
  public Set<PotionEffectType> getBeneficial() {
    return ImmutableSet.<PotionEffectType>builder()
        .addAll(super.getBeneficial())
        .add(PotionEffectType.BREATH_OF_THE_NAUTILUS)
        .add(PotionEffectType.CONDUIT_POWER)
        .add(PotionEffectType.DOLPHINS_GRACE)
        .add(PotionEffectType.HERO_OF_THE_VILLAGE)
        .add(PotionEffectType.LUCK)
        .add(PotionEffectType.SLOW_FALLING)
        .build();
  }

  @Override
  public Set<PotionEffectType> getHarmful() {
    return ImmutableSet.<PotionEffectType>builder()
        .addAll(super.getHarmful())
        .add(PotionEffectType.DARKNESS)
        .add(PotionEffectType.INFESTED)
        .add(PotionEffectType.LEVITATION)
        .add(PotionEffectType.OOZING)
        .add(PotionEffectType.UNLUCK)
        .add(PotionEffectType.WEAVING)
        .add(PotionEffectType.WIND_CHARGED)
        .build();
  }
}
