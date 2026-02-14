package tc.oc.pgm.platform.modern.modules.trim;

import static net.kyori.adventure.text.format.TextColor.color;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.HSVLike;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import tc.oc.pgm.util.StringUtils;

public class Trims {
  private static final Map<String, TrimPattern> PATTERNS_BY_KEY = new HashMap<>();
  private static final List<MaterialColor> TRIM_COLORS = new MaterialsBuilder().build();

  static {
    var patterns = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN);
    patterns.keyStream().forEach(key -> {
      var pattern = patterns.getOrThrow(key);
      PATTERNS_BY_KEY.put(StringUtils.simplify(key.asMinimalString()), pattern);
      PATTERNS_BY_KEY.put(StringUtils.simplify(key.asString()), pattern);
    });
  }

  public static TrimPattern getPatternByKey(String key) {
    return PATTERNS_BY_KEY.get(StringUtils.simplify(key.toLowerCase(Locale.ROOT)));
  }

  public static TrimMaterial getNearestMaterial(TextColor color) {
    var toMatch = color.asHSV();
    float bestDist = Float.MAX_VALUE;
    MaterialColor best = TRIM_COLORS.getFirst();

    for (MaterialColor potential : TRIM_COLORS) {
      float distance = distance(toMatch, potential);
      if (distance < bestDist) {
        best = potential;
        bestDist = distance;
      }
    }
    return best.material();
  }

  /*
   * Color distance formula in the HSV space, weighted so hue is the most important & saturation is partly ignored
   * This is
   */
  private static float distance(final HSVLike self, final HSVLike other) {
    float hd =
        5.0f * Math.min(Math.abs(self.h() - other.h()), 1.0F - Math.abs(self.h() - other.h()));
    float sd = 0.5f * (self.s() - other.s());
    float vd = (self.v() - other.v());
    return hd * hd + sd * sd + vd * vd;
  }

  private record MaterialColor(TrimMaterial material, float h, float s, float v)
      implements HSVLike {}

  private static class MaterialsBuilder {
    private final List<MaterialColor> materialColors = new ArrayList<>();

    List<MaterialColor> build() {
      // Palette from https://minecraft.wiki/w/Smithing#Material (2nd color)
      register(TrimMaterial.AMETHYST, color(0x9A5CC6));
      register(TrimMaterial.COPPER, color(0xB4684D));
      register(TrimMaterial.DIAMOND, color(0x6EECD2));
      register(TrimMaterial.EMERALD, color(0x0EC754));
      register(TrimMaterial.GOLD, color(0xECD93F));
      register(TrimMaterial.IRON, color(0xBFC9C8));
      register(TrimMaterial.LAPIS, color(0x1C4D9C));
      register(TrimMaterial.QUARTZ, color(0xF6EADF));
      register(TrimMaterial.NETHERITE, color(0x443A3B));
      register(TrimMaterial.REDSTONE, color(0xBD2008));
      register(TrimMaterial.RESIN, color(0xF69F3B));
      return List.copyOf(this.materialColors);
    }

    private void register(TrimMaterial material, TextColor color) {
      var hsv = color.asHSV();
      this.materialColors.add(new MaterialColor(material, hsv.h(), hsv.s(), hsv.v()));
    }
  }
}
