package tc.oc.pgm.server;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.PluginEvent;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import tc.oc.pgm.util.reflect.ReflectionUtils;

/**
 * A {@link PluginLoader} that allows for {@link Plugin}s to be loaded at runtime without a
 * {@link File}.
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class RuntimePluginLoader implements PluginLoader {

  private final Server server;
  private final JavaPluginLoader loader;

  public RuntimePluginLoader(Server server) {
    this.server = assertNotNull(server);
    this.loader = new JavaPluginLoader(server);
  }

  public Plugin loadPlugin(PluginDescriptionFile plugin) throws UnknownDependencyException {
    final SimplePluginManager manager = (SimplePluginManager) server.getPluginManager();
    try {
      final File file = new File("plugins", plugin.getName());
      final Class[] init =
          new Class[] {JavaPluginLoader.class, PluginDescriptionFile.class, File.class, File.class};
      final JavaPlugin instance = (JavaPlugin) Class.forName(plugin.getMain())
          .getConstructor(init)
          .newInstance(loader, plugin, file, file);
      ReflectionUtils.setField(instance, this, JavaPlugin.class.getDeclaredField("loader"));

      ReflectionUtils.readField(SimplePluginManager.class, manager, List.class, "plugins")
          .add(instance);
      ReflectionUtils.readField(SimplePluginManager.class, manager, Map.class, "lookupNames")
          .put(instance.getName().toLowerCase(), instance);

      return instance;
    } catch (Throwable t) {
      throw new UnknownDependencyException(
          t, "Unable to load plugin: " + plugin.getName() + " (" + plugin.getMain() + ")");
    }
  }

  @Override
  public Plugin loadPlugin(File file) throws UnknownDependencyException, InvalidPluginException {
    return loader.loadPlugin(file);
  }

  @Override
  public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
    return loader.getPluginDescription(file);
  }

  @Override
  public Pattern[] getPluginFileFilters() {
    return loader.getPluginFileFilters();
  }

  @Override
  public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(
      Listener listener, Plugin plugin) {
    return loader.createRegisteredListeners(listener, plugin);
  }

  public boolean togglePlugin(Plugin plugin, boolean on) {
    if (plugin.isEnabled() == on) return true;

    try {
      final Field enabled = JavaPlugin.class.getDeclaredField("isEnabled");
      enabled.setAccessible(true);

      if (on) {
        enabled.setBoolean(plugin, true);
        plugin.onEnable();
      } else {
        plugin.onDisable();
        enabled.setBoolean(plugin, false);
      }

      final PluginEvent event = on ? new PluginEnableEvent(plugin) : new PluginDisableEvent(plugin);
      server.getPluginManager().callEvent(event);

      return true;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      server.getLogger().log(Level.SEVERE, "Could not toggle plugin state", e);
    } catch (Throwable t) {
      server
          .getLogger()
          .log(
              Level.WARNING,
              "Could not " + (on ? "load" : "unload") + " plugin " + plugin.getName(),
              t);
    }

    return false;
  }

  @Override
  public void enablePlugin(Plugin plugin) {
    if (!togglePlugin(plugin, true)) {
      disablePlugin(plugin);
    }
  }

  @Override
  public void disablePlugin(Plugin plugin) {
    togglePlugin(plugin, false);
  }
}
