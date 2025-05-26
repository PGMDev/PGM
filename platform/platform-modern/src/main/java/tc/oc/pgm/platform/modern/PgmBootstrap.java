package tc.oc.pgm.platform.modern;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class PgmBootstrap implements PluginBootstrap {

  private static final String NAMESPACE = "pgm";
  private static final String PATH = "legacy_overworld";

  public static final ResourceKey<DimensionType> LEGACY_OVERWORLD = ResourceKey.create(
      Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(NAMESPACE, PATH));

  @Override
  public void bootstrap(@NotNull BootstrapContext context) {
    // Register the compatibility datapack
    context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, e -> {
      var registrar = e.registrar();
      try {
        final var uri = Objects.requireNonNull(
                PgmBootstrap.class.getResource("/pgm_compat_datapack"))
            .toURI();
        registrar.discoverPack(uri, "pgmCompat");
      } catch (URISyntaxException | IOException ex) {
        throw new RuntimeException(ex);
      }
    });
  }
}
