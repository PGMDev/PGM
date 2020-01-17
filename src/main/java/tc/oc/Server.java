package tc.oc;

import com.google.common.collect.Lists;
import net.kencochrane.raven.dsn.Dsn;
import net.kencochrane.raven.event.EventBuilder;
import net.kencochrane.raven.event.interfaces.ExceptionInterface;
import net.kencochrane.raven.event.interfaces.StackTraceInterface;
import net.kencochrane.raven.log4j2.SentryAppender;
import net.minecraft.server.v1_8_R3.DedicatedPlayerList;
import net.minecraft.server.v1_8_R3.DedicatedServer;
import net.minecraft.server.v1_8_R3.DispenserRegistry;
import net.minecraft.server.v1_8_R3.MinecraftEncryption;
import net.minecraft.server.v1_8_R3.PropertyManager;
import net.minecraft.server.v1_8_R3.WorldLoaderServer;
import net.minecraft.server.v1_8_R3.WorldServer;
import net.minecraft.server.v1_8_R3.WorldSettings;
import net.minecraft.server.v1_8_R3.WorldType;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.bukkit.Physical;
import org.bukkit.craftbukkit.libs.joptsimple.OptionParser;
import org.bukkit.craftbukkit.v1_8_R3.LoggerOutputStream;
import org.bukkit.craftbukkit.v1_8_R3.util.ForwardLogHandler;
import org.bukkit.craftbukkit.v1_8_R3.util.TerminalConsoleWriterThread;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.PluginEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoadOrder;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.fusesource.jansi.AnsiConsole;
import org.spigotmc.SpigotConfig;
import tc.oc.pgm.PGMImpl;
import tc.oc.pgm.api.PGM;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.regex.Pattern;

import static tc.oc.util.reflect.ReflectionUtils.readField;

/** Embedded {@link org.bukkit.Bukkit} server that natively runs {@link PGM}. */
public class Server extends DedicatedServer {

  public static void main(String[] args) {
    init(PGMImpl.class);
  }

  public static void init(Class<? extends JavaPlugin>... plugins) {
    try {
      new Server(plugins).primaryThread.start();
      Thread.sleep(Long.MAX_VALUE);
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

  private final Class<? extends JavaPlugin>[] plugins;
  private Logger logger;

  private Server(Class<? extends JavaPlugin>... plugins) {
    super(
        new OptionParser() {
          {
            this.acceptsAll(Lists.newArrayList("config"), "")
                .withRequiredArg()
                .ofType(File.class)
                .defaultsTo(new File("server.properties"), new File[0]);
            this.acceptsAll(Lists.newArrayList("sportpaper-settings"), "")
                .withRequiredArg()
                .ofType(File.class)
                .defaultsTo(new File("sportpaper.yml"), new File[0]);
            this.acceptsAll(Lists.newArrayList("plugins"), "")
                .withRequiredArg()
                .ofType(File.class)
                .defaultsTo(new File("plugins"), new File[0]);
          }
        }.parse());
    this.plugins = plugins;
  }

  @Override
  protected boolean init() throws IOException {
    logger = setupLogger();

    setupProperties();
    setupMode();
    setupServer();
    setupPlugins();
    setupWorld();
    setupListener();
    setupConsole();
    setupMode();

    return true;
  }

  private Logger setupLogger() {
    AnsiConsole.systemInstall();

    final java.util.logging.Logger global = java.util.logging.Logger.getLogger("");
    global.setUseParentHandlers(false);

    final Handler[] handlers = global.getHandlers();
    for (int i = 0; i < handlers.length; ++i) {
      global.removeHandler(handlers[i]);
    }

    global.addHandler(new ForwardLogHandler());
    Logger logger = (Logger) LogManager.getRootLogger();

    final Iterator<org.apache.logging.log4j.core.Appender> appenders =
        logger.getAppenders().values().iterator();
    while (appenders.hasNext()) {
      final org.apache.logging.log4j.core.Appender appender = appenders.next();
      if (appender instanceof ConsoleAppender) {
        logger.removeAppender(appender);
      }
    }

    if (Dsn.dsnLookup() != null) {
      logger.addAppender(new Appender());
    }

    new Thread(new TerminalConsoleWriterThread(System.out, this.reader)).start();

    System.setOut(new PrintStream(new LoggerOutputStream(logger, Level.INFO), true));
    System.setErr(new PrintStream(new LoggerOutputStream(logger, Level.WARN), true));

    try {
      return (Logger) Server.class.getField("LOGGER").get(Server.class);
    } catch (IllegalAccessException | NoSuchFieldException e) {
    }

    return logger;
  }

  private void setupConsole() {
    final Thread console =
        new Thread(
            () -> {
              try {
                while (!isStopped() && isRunning()) {
                  final String command = reader.readLine("> ", null);
                  if (command != null && !command.trim().isEmpty()) {
                    issueCommand(command, this);
                  }
                }
              } catch (IOException io) {
                safeShutdown();
              }
            });
    console.setDaemon(true);
    console.start();
  }

  private void setupProperties() {
    propertyManager = new PropertyManager(options);

    setSpawnAnimals(propertyManager.getBoolean("spawn-animals", true));
    setSpawnNPCs(propertyManager.getBoolean("spawn-npcs", true));
    setPVP(propertyManager.getBoolean("pvp", true));
    setAllowFlight(propertyManager.getBoolean("allow-flight", false));
    setResourcePack(
        propertyManager.getString("resource-pack", ""),
        propertyManager.getString("resource-pack-hash", ""));
    setGamemode(WorldSettings.EnumGamemode.getById(propertyManager.getInt("gamemode", 0)));
    setForceGamemode(propertyManager.getBoolean("force-gamemode", false));
    setIdleTimeout(propertyManager.getInt("player-idle-timeout", 0));
    setOnlineMode(propertyManager.getBoolean("online-mode", true));
    setMotd(propertyManager.getString("motd", "A Minecraft 1.8 Server"));
    setWorld(propertyManager.getString("level-name", "world"));
    setPort(propertyManager.getInt("port", 25565));
    c(propertyManager.getInt("max-build-height", 256)); // setBuildHeight
    c(propertyManager.getString("server-ip", "0.0.0.0")); // setServerIp
  }

  private void setupMode() {
    final String mode = System.getenv("LOGIN_MODE");
    if (mode != null) {
      final boolean online = mode.equalsIgnoreCase("online");
      propertyManager.setProperty("online-mode", online);
      setOnlineMode(online);

      SpigotConfig.bungee = mode.equalsIgnoreCase("bungee");
    }
  }

  private void setupServer() {
    a(new DedicatedPlayerList(this));
    a(MinecraftEncryption.b());
    DispenserRegistry.c();
  }

  private void setupPlugins() {
    server.loadPlugins();

    final JavaPlugin rewind = (JavaPlugin) server.getPluginManager().getPlugin("ViaRewind");
    if (rewind != null) {
      new Loader().togglePlugin(rewind, true);
    }

    for (Class<? extends JavaPlugin> plugin : plugins) {
      setupPlugin(plugin);
    }
    server.enablePlugins(PluginLoadOrder.POSTWORLD);
  }

  @SuppressWarnings("unchecked")
  private void setupPlugin(Class<? extends JavaPlugin> mainClass) {
    final SimplePluginManager manager = (SimplePluginManager) server.getPluginManager();
    try {
      final String name = mainClass.getSimpleName().replace("Plugin", "").replace("Impl", "");
      final PluginDescriptionFile description =
          new PluginDescriptionFile(name, "unknown", mainClass.getName());
      final File file = new File("plugins", name);

      final Class[] init =
          new Class[] {
            PluginLoader.class,
            org.bukkit.Server.class,
            PluginDescriptionFile.class,
            File.class,
            File.class
          };
      final JavaPlugin plugin =
          mainClass.getConstructor(init).newInstance(new Loader(), server, description, file, file);

      readField(SimplePluginManager.class, manager, List.class, "plugins").add(plugin);
      readField(SimplePluginManager.class, manager, Map.class, "lookupNames")
          .put(plugin.getName().toLowerCase(), plugin);
    } catch (IllegalAccessException
        | InstantiationException
        | NoSuchMethodException
        | InvocationTargetException e) {
      logger.fatal("Could not build plugin '" + mainClass.getName() + "'", e);
    }
  }

  private void setupWorld() {
    convertable = new WorldLoaderServer(server.getWorldContainer());

    final long start = System.nanoTime();
    a(U(), U(), 0 /* seed */, WorldType.FLAT, "" /* generator */);
    final long duration = System.nanoTime() - start;

    try {
      final Field dimension = WorldServer.class.getDeclaredField("dimension");
      dimension.setAccessible(true);

      final Field modifiers = Field.class.getDeclaredField("modifiers");
      modifiers.setAccessible(true);
      modifiers.setInt(dimension, dimension.getModifiers() & ~Modifier.FINAL);

      dimension.set(worlds.get(0), 11);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      logger.warn("Could not change dimension of default world", e);
    }

    logger.info(
        "Done ("
            + String.format("%.3fs", (double) duration / 1.0E9D)
            + ")! For help, type \"help\" or \"?\"");
  }

  private void setupListener() throws IOException {
    aq().a(InetAddress.getByName(getServerIp()), R());
  }

  class Loader implements PluginLoader {

    private final PluginLoader loader = new JavaPluginLoader(server);

    @Override
    public Plugin loadPlugin(File file) throws UnknownDependencyException {
      throw new UnsupportedOperationException();
    }

    @Override
    public PluginDescriptionFile getPluginDescription(File file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Pattern[] getPluginFileFilters() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(
        Listener listener, Plugin plugin) {
      return loader.createRegisteredListeners(listener, plugin);
    }

    private boolean togglePlugin(Plugin plugin, boolean on) {
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

        final PluginEvent event =
            on ? new PluginEnableEvent(plugin) : new PluginDisableEvent(plugin);
        server.getPluginManager().callEvent(event);

        return true;
      } catch (NoSuchFieldException | IllegalAccessException e) {
        logger.fatal("Could not toggle plugin state", e);
      } catch (Throwable t) {
        logger.fatal("Could not build plugin " + plugin.getName(), t);
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

  class Appender extends SentryAppender {
    Appender() {
      this.start();
    }

    @Override
    public boolean isFiltered(LogEvent event) {
      return !event.getLevel().lessOrEqual(Level.WARN);
    }

    @Override
    protected net.kencochrane.raven.event.Event buildEvent(LogEvent event) {
      final Throwable err = event.getThrown();
      final EventBuilder builder = new EventBuilder();

      builder.setLevel(formatLevel(event.getLevel()));
      builder.setTimestamp(new Date(event.getMillis()));
      builder.setMessage(event.getMessage().getFormattedMessage());

      if (err != null) {
        builder.addSentryInterface(new ExceptionInterface(err));
      } else if (event.getSource() != null) {
        builder.addSentryInterface(
            new StackTraceInterface(new StackTraceElement[] {event.getSource()}));
      }

      if (err instanceof EventException) {
        final Event e = ((EventException) err).getEvent();
        builder.addExtra("event", e);
        if (e instanceof Physical) {
          builder.addExtra("world", ((Physical) e).getWorld().getName());
        }
        if (e instanceof PlayerEvent) {
          builder.addExtra("player", ((PlayerEvent) e).getPlayer().getName());
        } else if (e instanceof EntityEvent) {
          builder.addExtra("entity", ((EntityEvent) e).getEntityType());
        } else if (e instanceof InventoryEvent) {
          builder.addExtra("inventory", ((InventoryEvent) e).getInventory().getName());
        }
      }

      builder.addTag("os", System.getProperty("os.name"));
      builder.addTag("java", System.getProperty("java.version"));
      builder.addTag("server", Server.this.getServerModName() + "-" + Server.this.getVersion());
      if (Server.this.server != null) {
        for (Plugin plugin : Server.this.server.getPluginManager().getPlugins()) {
          builder.addTag(
              "plugin." + plugin.getName().toLowerCase(), plugin.getDescription().getVersion());
        }
      }

      raven.runBuilderHelpers(builder);
      return builder.build();
    }
  }
}
