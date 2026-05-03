package tc.oc.pgm.platform.modern;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.ObjectType;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.plugin.manager.PaperPluginManagerImpl;
import io.papermc.paper.plugin.provider.classloader.PaperClassLoaderStorage;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.PluginClassLoader;
import org.jspecify.annotations.NonNull;
import tc.oc.pgm.util.DataVersions;

@SuppressWarnings("UnstableApiUsage")
public class PgmBootstrap implements PluginBootstrap {

  private static final String NAMESPACE = "pgm";
  private static final String PATH = "legacy_overworld";

  public static final ResourceKey<DimensionType> LEGACY_OVERWORLD = ResourceKey.create(
      Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(NAMESPACE, PATH));

  @Override
  public void bootstrap(@NonNull BootstrapContext context) {
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

    // Set legacy maps to the datapack-provided legacy overworld dimension
    MCTypeRegistry.CHUNK.addStructureConverter(new DataConverter<>(DataVersions.V1_18_EXP_1) {
      @Override
      public MapType convert(MapType data, final long sourceVersion, final long toVersion) {
        final MapType level = data.getMap("Level");
        if (level == null) return data;
        final MapType context = data.getMap("__context"); // Passed through by ChunkStorage
        if (context == null) return data;
        if ("minecraft:overworld".equals(context.getString("dimension", ""))) {
          context.setString("dimension", "pgm:legacy_overworld");
        }
        return data;
      }
    });

    // Mark all legacy leaves as persistent to avoid decay
    MCTypeRegistry.CHUNK.addStructureConverter(new DataConverter<>(DataVersions.V18W21B) {
      @Override
      public MapType convert(MapType data, final long sourceVersion, final long toVersion) {
        final MapType level = data.getMap("Level");
        if (level == null) return data;
        final ListType sections = level.getList("Sections", ObjectType.MAP);
        if (sections == null) return data;
        for (int i = 0; i < sections.size(); i++) {
          final MapType section = sections.getMap(i);
          final ListType palette = section.getList("Palette", ObjectType.MAP);
          if (palette == null) continue;
          for (int j = 0; j < palette.size(); j++) {
            final MapType entry = palette.getMap(j);
            final String name = entry.getString("Name", "");
            if (!name.endsWith("_leaves")) continue;
            entry.getOrCreateMap("Properties").setString("persistent", "true");
          }
        }
        return data;
      }
    });
  }

  @Override
  public @NonNull JavaPlugin createPlugin(PluginProviderContext context) {
    var ourClassLoader = (PaperPluginClassLoader) getClass().getClassLoader();
    var pluginMeta = (PaperPluginMeta) ourClassLoader.getConfiguration();
    var descriptor = new PluginDescriptionFile(
        pluginMeta.getName(),
        pluginMeta.getName(),
        pluginMeta.getProvidedPlugins(),
        pluginMeta.getMainClass(),
        null,
        pluginMeta.getPluginDependencies(),
        pluginMeta.getPluginSoftDependencies(),
        pluginMeta.getLoadBeforePlugins(),
        pluginMeta.getVersion(),
        Map.of(),
        pluginMeta.getDescription(),
        pluginMeta.getAuthors(),
        pluginMeta.getContributors(),
        pluginMeta.getWebsite(),
        pluginMeta.getLoggerPrefix(),
        pluginMeta.getLoadOrder(),
        pluginMeta.getPermissions(),
        pluginMeta.getPermissionDefault(),
        Set.of(),
        pluginMeta.getAPIVersion(),
        List.of());

    try {
      // Unregister the bootstrap classloader to make sure the PluginClassLoader below:
      // - doesn't loop into itself, causing a stack overflow
      // - is used by 3rd-party integration plugins to load PGM classes
      PaperClassLoaderStorage.instance().unregisterClassloader(ourClassLoader);

      var jar = new JarFile(context.getPluginSource().toFile());
      var classLoader = new PluginClassLoader(
          ourClassLoader.getParent(),
          descriptor,
          context.getDataDirectory().toFile(),
          context.getPluginSource().toFile(),
          ourClassLoader,
          jar,
          PaperPluginManagerImpl.getInstance());

      return Objects.requireNonNull(classLoader.getPlugin());
    } catch (IOException | InvalidPluginException e) {
      throw new RuntimeException(e);
    }
  }
}
