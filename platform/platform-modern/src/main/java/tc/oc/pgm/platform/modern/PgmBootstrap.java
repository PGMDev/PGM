package tc.oc.pgm.platform.modern;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.TypedKey;
import java.util.OptionalLong;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.platform.modern.registry.PgmRegistryEvents;
import tc.oc.pgm.platform.modern.registry.PgmRegistryKey;

@SuppressWarnings("UnstableApiUsage")
public class PgmBootstrap implements PluginBootstrap {

  private static final String NAMESPACE = "pgm";
  private static final String PATH = "legacy_overworld";

  public static final ResourceKey<DimensionType> LEGACY_OVERWORLD = ResourceKey.create(
      Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(NAMESPACE, PATH));

  @Override
  public void bootstrap(@NotNull BootstrapContext context) {
    // Registering is required here, as the server will sync the registries with the client
    context
        .getLifecycleManager()
        .registerEventHandler(PgmRegistryEvents.DIMENSION_TYPE.freeze().newHandler(e -> e.registry()
            .register(
                TypedKey.create(PgmRegistryKey.DIMENSION_TYPE, Key.key(NAMESPACE, PATH)),
                b -> b.setDimension(new DimensionType(
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
                    BlockTags.INFINIBURN_OVERWORLD,
                    BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                    0.0f,
                    new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0))))));

    // 1.17 worlds get pgm:legacy_overworld dimension, which limits world height 0 to 255
    int VERSION = MCVersions.V1_17_1 + 95;
    MCTypeRegistry.CHUNK.addStructureConverter(new DataConverter<>(VERSION) {
      @Override
      public MapType<String> convert(
          final MapType<String> data, final long sourceVersion, final long toVersion) {
        final MapType<String> level = data.getMap("Level");
        if (level == null) return data;
        final MapType<String> context = data.getMap("__context"); // Passed through by ChunkStorage
        if (context == null) return data;
        if ("minecraft:overworld".equals(context.getString("dimension", ""))) {
          context.setString("dimension", "pgm:legacy_overworld");
        }
        return data;
      }
    });
  }
}
