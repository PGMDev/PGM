package tc.oc.pgm.platform.modern;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEventType;
import io.papermc.paper.registry.PaperRegistries;
import io.papermc.paper.registry.PaperRegistryBuilder;
import io.papermc.paper.registry.PaperRegistryListenerManager;
import io.papermc.paper.registry.RegistryBuilder;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.util.Conversions;
import io.papermc.paper.registry.entry.RegistryEntry;
import io.papermc.paper.registry.event.RegistryEventProvider;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.event.type.RegistryEntryAddEventType;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.DimensionType;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.reflect.ReflectionUtils;

@SuppressWarnings("UnstableApiUsage")
public class PgmBootstrap implements PluginBootstrap {

  public static final ResourceKey<DimensionType> LEGACY_OVERWORLD = ResourceKey.create(
      Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath("pgm", "legacy_overworld"));

  @Override
  @SuppressWarnings("unchecked")
  public void bootstrap(@NotNull BootstrapContext context) {

    var regKeyField = ReflectionUtils.getField(PaperRegistries.class, "BY_REGISTRY_KEY");
    var resKeyField = ReflectionUtils.getField(PaperRegistries.class, "BY_RESOURCE_KEY");
    var byRegistryKey = new HashMap<>(
        (Map<RegistryKey<?>, RegistryEntry<?, ?>>) ReflectionUtils.readField(null, regKeyField));
    var byResourceKey = new HashMap<>(
        (Map<ResourceKey<?>, RegistryEntry<?, ?>>) ReflectionUtils.readField(null, resKeyField));
    ReflectionUtils.setFinalStatic(regKeyField, byRegistryKey);
    ReflectionUtils.setFinalStatic(resKeyField, byResourceKey);

    var create = ReflectionUtils.getMethod(
        ReflectionUtils.getClassFromName("io.papermc.paper.registry.RegistryKeyImpl"),
        "create",
        String.class);

    RegistryKey<BukkitDimensionType> registryKey = (RegistryKey<BukkitDimensionType>)
        ReflectionUtils.callMethod(create, null, "dimension_type");

    var entry = RegistryEntry.writable(
        Registries.DIMENSION_TYPE,
        registryKey,
        DimensionType.class,
        BukkitDimensionType::new,
        DimensionTypeBuilder::new);

    byResourceKey.put(Registries.DIMENSION_TYPE, entry);
    byRegistryKey.put(registryKey, entry);

    RegistryEventProvider<BukkitDimensionType, DimensionTypeBuilder> EVENT =
        RegistryEventProviderImpl.create(registryKey);

    context.getLifecycleManager().registerEventHandler(EVENT.freeze().newHandler(e -> {
      e.registry()
          .register(
              TypedKey.create(registryKey, Key.key("pgm:legacy_overworld")),
              b -> b.withDimension(new DimensionType(
                  OptionalLong.empty(),
                  true,
                  false,
                  false,
                  true,
                  1.0,
                  true,
                  false,
                  0, // Min height = 0
                  256,
                  256,
                  TagKey.create(
                      Registries.BLOCK,
                      ResourceLocation.withDefaultNamespace("infiniburn_overworld")),
                  ResourceLocation.withDefaultNamespace("overworld"),
                  0.0f,
                  new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0))));

      /*Registry.register(
      registryAccess.registryOrThrow(Registries.DIMENSION_TYPE),
      LEGACY_OVERWORLD,
      dimensionType);*/

      /*Registry.register(
      registryAccess.registryOrThrow(Registries.LEVEL_STEM),
      ResourceLocation.fromNamespaceAndPath("pgm", "legacy_overworld"),
      new LevelStem(Holder.direct(dimensionType), null));*/
    }));
  }

  public record BukkitDimensionType(NamespacedKey key, DimensionType dimension) implements Keyed {
    @Override
    public @NotNull NamespacedKey getKey() {
      return key;
    }
  }

  public static class DimensionTypeBuilder
      implements PaperRegistryBuilder<DimensionType, BukkitDimensionType> {
    private DimensionType dimension;

    private DimensionTypeBuilder(
        Conversions conversions,
        TypedKey<BukkitDimensionType> typedKey,
        @Nullable DimensionType dimensionType) {
      this.dimension = dimensionType;
    }

    public DimensionTypeBuilder withDimension(DimensionType dimension) {
      this.dimension = dimension;
      return this;
    }

    @Override
    public DimensionType build() {
      return dimension;
    }
  }

  record RegistryEventProviderImpl<T, B extends RegistryBuilder<T>>(RegistryKey<T> registryKey)
      implements RegistryEventProvider<T, B> {
    RegistryEventProviderImpl(RegistryKey<T> registryKey) {
      this.registryKey = registryKey;
    }

    static <T, B extends RegistryBuilder<T>> RegistryEventProvider<T, B> create(
        RegistryKey<T> registryKey) {
      return new RegistryEventProviderImpl(registryKey);
    }

    public RegistryEntryAddEventType<T, B> entryAdd() {
      return PaperRegistryListenerManager.INSTANCE.getRegistryValueAddEventType(this);
    }

    public LifecycleEventType.Prioritizable<BootstrapContext, RegistryFreezeEvent<T, B>> freeze() {
      return PaperRegistryListenerManager.INSTANCE.getRegistryFreezeEventType(this);
    }

    public RegistryKey<T> registryKey() {
      return this.registryKey;
    }
  }
}
