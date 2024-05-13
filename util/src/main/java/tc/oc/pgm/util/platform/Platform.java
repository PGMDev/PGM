package tc.oc.pgm.util.platform;

import static org.reflections.scanners.Scanners.TypesAnnotated;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.reflect.ReflectionUtils;
import tc.oc.pgm.util.text.TextParser;

@SuppressWarnings("unchecked")
public abstract class Platform {
  public static final String SERVER_VERSION =
      Bukkit.getServer().getVersion().toLowerCase(Locale.ROOT);
  public static final Version MINECRAFT_VERSION =
      TextParser.parseVersion(Bukkit.getServer().getBukkitVersion().split("-")[0]);

  private static final Reflections REFLECTIONS =
      new Reflections(
          new ConfigurationBuilder().forPackage("tc.oc.pgm.platform").setScanners(TypesAnnotated));
  private static final Map<Class<?>, Object> INSTANCES = new HashMap<>();

  public static final @NotNull Manifest MANIFEST = requireInstance(Manifest.class);

  public static <T> @NotNull T requireInstance(Class<T> clazz) {
    return (T) INSTANCES.computeIfAbsent(clazz, cl -> getBestSupported(cl, true));
  }

  public static @Nullable <T> T getInstance(Class<T> clazz) {
    return (T) INSTANCES.computeIfAbsent(clazz, cl -> getBestSupported(cl, false));
  }

  public static @Nullable <T> Optional<T> optionalInstance(Class<T> clazz) {
    return Optional.ofNullable(
        (T) INSTANCES.computeIfAbsent(clazz, cl -> getBestSupported(cl, false)));
  }

  public static boolean isCurrentVariant(Supports.Variant variant) {
    return SERVER_VERSION.contains(variant.name().toLowerCase(Locale.ROOT));
  }

  private static <T> Iterable<Class<?>> getSupported(Class<T> parent) {
    return REFLECTIONS.get(
        TypesAnnotated.with(Supports.class, Supports.List.class)
            .asClass()
            .filter(parent::isAssignableFrom));
  }

  @Contract(pure = true, value = "_, true -> !null")
  private static Object getBestSupported(Class<?> parent, boolean required) {
    Class<?> result = null;
    Supports.Priority priority = null;
    for (Class<?> clazz : getSupported(parent)) {
      Supports[] supportList = clazz.getDeclaredAnnotationsByType(Supports.class);
      for (Supports sup : supportList) {
        if (!isCurrentVariant(sup.value())) continue;
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
    if (result == null && required)
      throw new UnsupportedOperationException(
          "Current server software platform does not have an impl for: " + parent.getSimpleName());
    if (result == null) return null;

    return ReflectionUtils.callConstructor(ReflectionUtils.getConstructor(result));
  }

  public interface Manifest {
    void onEnable(Plugin plugin);
  }
}
