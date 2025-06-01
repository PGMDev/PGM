package tc.oc.pgm.platform.modern.util;

import com.google.common.io.ByteStreams;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.PluginClassLoaderGroup;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A class loader with excerpts from {@link org.bukkit.plugin.java.PluginClassLoader} to route
 * loaded classes through {@link org.bukkit.craftbukkit.util.Commodore} remapping
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
@NullMarked
public class PGMClassLoader extends URLClassLoader implements ConfiguredPluginClassLoader {
  private final URL url;
  private final JarFile jar;
  private final PluginDescriptionFile descriptor;
  private final Manifest manifest;
  private final Map<String, Class<?>> loadedClasses = new HashMap<>();
  private final PaperPluginClassLoader ourClassLoader =
      (PaperPluginClassLoader) getClass().getClassLoader();

  public PGMClassLoader(URL url, JarFile jar, PluginDescriptionFile descriptor) throws IOException {
    super(new URL[] {url}, PGMClassLoader.class.getClassLoader().getParent());
    this.url = url;
    this.jar = jar;
    this.manifest = jar.getManifest();
    this.descriptor = descriptor;
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    return loadClass(name, resolve, true, true);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    if (name.startsWith("org.bukkit.") || name.startsWith("net.minecraft.")) {
      throw new ClassNotFoundException(name);
    }
    Class<?> result = loadedClasses.get(name);

    if (result == null) {
      String path = name.replace('.', '/').concat(".class");
      JarEntry entry = jar.getJarEntry(path);

      if (entry != null) {
        byte[] classBytes;

        try (InputStream is = jar.getInputStream(entry)) {
          classBytes = ByteStreams.toByteArray(is);
        } catch (IOException ex) {
          throw new ClassNotFoundException(name, ex);
        }

        classBytes = org.bukkit.Bukkit.getServer()
            .getUnsafe()
            .processClass(descriptor, path, classBytes); // Paper

        int dot = name.lastIndexOf('.');
        if (dot != -1) {
          String pkgName = name.substring(0, dot);
          if (getPackage(pkgName) == null) {
            try {
              if (manifest != null) {
                definePackage(pkgName, manifest, url);
              } else {
                definePackage(pkgName, null, null, null, null, null, null, null);
              }
            } catch (IllegalArgumentException ex) {
              if (getPackage(pkgName) == null) {
                throw new IllegalStateException("Cannot find package " + pkgName);
              }
            }
          }
        }

        CodeSigner[] signers = entry.getCodeSigners();
        CodeSource source = new CodeSource(url, signers);

        result = defineClass(name, classBytes, 0, classBytes.length, source);
      }

      if (result == null) {
        result = super.findClass(name);
      }

      loadedClasses.put(name, result);
    }
    return result;
  }

  @Override
  public PluginMeta getConfiguration() {
    return ourClassLoader.getConfiguration();
  }

  @Override
  public Class<?> loadClass(String name, boolean resolve, boolean checkGroup, boolean checkLibs)
      throws ClassNotFoundException {
    try {
      Class<?> result = super.loadClass(name, resolve);

      if (result.getClassLoader() == this) {
        return result;
      }
    } catch (ClassNotFoundException ignored) {
    }

    return ourClassLoader.loadClass(name, resolve, checkGroup, checkLibs);
  }

  @Override
  public void init(JavaPlugin javaPlugin) {
    ourClassLoader.init(javaPlugin);
  }

  @Override
  public @Nullable JavaPlugin getPlugin() {
    return ourClassLoader.getPlugin();
  }

  @Override
  public @Nullable PluginClassLoaderGroup getGroup() {
    return ourClassLoader.getGroup();
  }
}
