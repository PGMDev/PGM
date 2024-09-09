package tc.oc.pgm.platform.modern.registry;

import io.papermc.paper.registry.PaperRegistries;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.entry.RegistryEntry;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import tc.oc.pgm.util.reflect.ReflectionUtils;

/**
 * Extension of Paper's {@link RegistryKey} constants to have unsupported keys. Also does the work
 * of inserting them into {@link PaperRegistries}'s maps
 */
public class PgmRegistryKey {
  public static final RegistryKey<CraftDimensionType> DIMENSION_TYPE = create("dimension_type");

  static {
    Map<RegistryKey<?>, RegistryEntry<?, ?>> byRegistryKey = getRegistryMap("BY_REGISTRY_KEY");
    Map<ResourceKey<?>, RegistryEntry<?, ?>> byResourceKey = getRegistryMap("BY_RESOURCE_KEY");

    List<RegistryEntry<?, ?>> entries = List.of(RegistryEntry.writable(
        Registries.DIMENSION_TYPE,
        PgmRegistryKey.DIMENSION_TYPE,
        CraftDimensionType.class, // Dummy class to load, has no bearing.
        CraftDimensionType::new,
        CraftDimensionType.Builder::new));

    for (RegistryEntry<?, ?> entry : entries) {
      byRegistryKey.put(entry.apiKey(), entry);
      byResourceKey.put(entry.mcKey(), entry);
    }
  }

  private static <T> RegistryKey<T> create(String name) {
    Class<?> impl = ReflectionUtils.getClassFromName("io.papermc.paper.registry.RegistryKeyImpl");
    Method createMethod = ReflectionUtils.getMethod(impl, "create", String.class);
    //noinspection unchecked
    return (RegistryKey<T>) ReflectionUtils.callMethod(createMethod, null, name);
  }

  private static <T> Map<T, RegistryEntry<?, ?>> getRegistryMap(String name) {
    var unmodifiableMap = ReflectionUtils.readField(PaperRegistries.class, null, Map.class, name);
    // Paper uses Collections#unmodifiableMap, to modify we need the inner map
    return UnsafeReflection.getFieldUnsafe(unmodifiableMap, "m");
  }
}
