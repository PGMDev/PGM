package tc.oc.pgm.platform.modern;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.TypedKey;
import java.util.OptionalLong;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.platform.modern.registry.PgmRegistryEvents;
import tc.oc.pgm.platform.modern.registry.PgmRegistryKey;

@SuppressWarnings("UnstableApiUsage")
public class PgmBootstrap implements PluginBootstrap {

  public static final ResourceKey<DimensionType> LEGACY_OVERWORLD = ResourceKey.create(
      Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath("pgm", "legacy_overworld"));

  @Override
  public void bootstrap(@NotNull BootstrapContext context) {
    context
        .getLifecycleManager()
        .registerEventHandler(PgmRegistryEvents.DIMENSION_TYPE.freeze().newHandler(e -> e.registry()
            .register(
                TypedKey.create(PgmRegistryKey.DIMENSION_TYPE, Key.key("pgm", "legacy_overworld")),
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
                    TagKey.create(
                        Registries.BLOCK,
                        ResourceLocation.withDefaultNamespace("infiniburn_overworld")),
                    ResourceLocation.withDefaultNamespace("overworld"),
                    0.0f,
                    new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0))))));
  }
}
