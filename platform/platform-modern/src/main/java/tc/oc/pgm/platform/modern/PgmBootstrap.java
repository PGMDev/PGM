package tc.oc.pgm.platform.modern;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.plugin.provider.classloader.PaperClassLoaderStorage;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Objects;
import java.util.jar.JarFile;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.platform.modern.dfu.PGMDataFixer;
import tc.oc.pgm.platform.modern.util.PGMClassLoader;
import tc.oc.pgm.util.reflect.ReflectionUtils;

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

    // Paper 1.21.5 uses DFU to upgrade worlds, hook into that instead
    PGMDataFixer.hookMojangDFU();
  }

  @Override
  public JavaPlugin createPlugin(PluginProviderContext context) {
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
        Collections.emptyMap(),
        pluginMeta.getDescription(),
        pluginMeta.getAuthors(),
        pluginMeta.getContributors(),
        pluginMeta.getWebsite(),
        pluginMeta.getLoggerPrefix(),
        pluginMeta.getLoadOrder(),
        pluginMeta.getPermissions(),
        pluginMeta.getPermissionDefault(),
        Collections.emptySet(),
        pluginMeta.getAPIVersion(),
        Collections.emptyList());

    try {
      var jar = new JarFile(context.getPluginSource().toFile());
      var url = context.getPluginSource().toUri().toURL();
      var classLoader = new PGMClassLoader(url, jar, descriptor);

      // Silence Paper warning about an unregistered class loader
      PaperClassLoaderStorage.instance().registerUnsafePlugin(classLoader);

      var pluginClass = Class.forName(pluginMeta.getMainClass(), true, classLoader);
      var constructor = ReflectionUtils.getConstructor(pluginClass);

      return (JavaPlugin) ReflectionUtils.callConstructor(constructor);
    } catch (ClassNotFoundException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
