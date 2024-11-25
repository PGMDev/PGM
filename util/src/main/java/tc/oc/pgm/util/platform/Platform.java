package tc.oc.pgm.util.platform;

import static org.reflections.scanners.Scanners.TypesAnnotated;
import static tc.oc.pgm.util.Assert.assertTrue;

import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.material.ColorUtils;
import tc.oc.pgm.util.material.MaterialUtils;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.platform.Supports.Variant;
import tc.oc.pgm.util.reflect.ReflectionUtils;
import tc.oc.pgm.util.text.TextParser;

@SuppressWarnings("unchecked")
public abstract class Platform {
  private static final Reflections REFLECTIONS = new Reflections(
      new ConfigurationBuilder().forPackage("tc.oc.pgm.platform").setScanners(TypesAnnotated));

  public static final Version MINECRAFT_VERSION;
  public static final Variant VARIANT;

  static {
    var sv = Bukkit.getServer();
    MINECRAFT_VERSION = TextParser.parseVersion(sv.getBukkitVersion().split("-")[0]);
    VARIANT = Arrays.stream(Variant.values())
        .filter(v -> v.matcher.test(sv))
        .findFirst()
        .orElse(null);
  }

  public static final @NotNull Manifest MANIFEST = get(Manifest.class);

  /**
   * Do a minimum sanity-check of the platform's viability and early-load some codepaths
   *
   * @throws Throwable could throw even class not found issues if loading in the wrong version
   */
  public static void init() throws Throwable {
    NMSHacks.NMS_HACKS.getTPS();

    var item = MaterialUtils.MATERIAL_UTILS.parseItemMaterialData("35:1", null);
    assertTrue(ColorUtils.COLOR_UTILS.isColorAffected(item.getItemType()));
  }

  public static <T> @NotNull T get(Class<T> clazz) {
    return (T) Platform.getBestSupported(clazz);
  }

  public static boolean isLegacy() {
    return VARIANT == Variant.SPORTPAPER;
  }

  public static boolean isModern() {
    return VARIANT == Variant.PAPER;
  }

  private static <T> Iterable<Class<?>> getSupported(Class<T> parent) {
    return REFLECTIONS.get(TypesAnnotated.with(Supports.class, Supports.List.class)
        .asClass()
        .filter(parent::isAssignableFrom));
  }

  private static Object getBestSupported(Class<?> parent) {
    Class<?> result = null;
    Supports.Priority priority = null;
    for (Class<?> clazz : getSupported(parent)) {
      Supports[] supportList = clazz.getDeclaredAnnotationsByType(Supports.class);
      for (Supports sup : supportList) {
        if (VARIANT != sup.value()) continue;
        if (!sup.minVersion().isEmpty()
            && MINECRAFT_VERSION.isOlderThan(TextParser.parseVersion(sup.minVersion()))) continue;
        if (!sup.maxVersion().isEmpty()
            && TextParser.parseVersion(sup.maxVersion()).isOlderThan(MINECRAFT_VERSION)) continue;

        if (priority == null || priority.compareTo(sup.priority()) < 0) {
          priority = sup.priority();
          result = clazz;
        }
      }
    }
    if (result == null)
      throw new UnsupportedOperationException(
          "Current server software platform does not have an impl for: " + parent.getSimpleName());

    return ReflectionUtils.callConstructor(ReflectionUtils.getConstructor(result));
  }

  public interface Manifest {
    void onEnable(Plugin plugin);
  }
}
