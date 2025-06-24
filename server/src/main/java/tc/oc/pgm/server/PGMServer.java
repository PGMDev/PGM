package tc.oc.pgm.server;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.logging.Handler;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.server.v1_8_R3.DedicatedPlayerList;
import net.minecraft.server.v1_8_R3.DedicatedServer;
import net.minecraft.server.v1_8_R3.DispenserRegistry;
import net.minecraft.server.v1_8_R3.MinecraftEncryption;
import net.minecraft.server.v1_8_R3.WorldLoaderServer;
import net.minecraft.server.v1_8_R3.WorldSettings;
import net.minecraft.server.v1_8_R3.WorldType;
import org.apache.log4j.BasicConfigurator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.bukkit.craftbukkit.libs.joptsimple.OptionParser;
import org.bukkit.craftbukkit.v1_8_R3.LoggerOutputStream;
import org.bukkit.craftbukkit.v1_8_R3.util.ForwardLogHandler;
import org.bukkit.craftbukkit.v1_8_R3.util.TerminalConsoleWriterThread;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoadOrder;
import org.fusesource.jansi.AnsiConsole;
import org.spigotmc.SpigotConfig;
import tc.oc.pgm.util.reflect.ReflectionUtils;

/**
 * Embedded {@link org.bukkit.Bukkit} server that natively runs plugins.
 *
 * <p>Most of this code is lifted from {@link DedicatedServer#init()}, and is broken up into
 * different methods to allow for customization.
 */
public class PGMServer extends DedicatedServer implements Runnable {

  public static void main(String[] args) throws InvalidDescriptionException {
    BasicConfigurator.configure();
    new PGMServer(new PluginDescriptionFile(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("plugin.yml")))
        .run();
  }

  protected String name;
  protected PluginDescriptionFile[] plugins;
  protected Logger logger;
  protected boolean init;

  public PGMServer(PluginDescriptionFile... plugins) {
    super(
        new OptionParser() {
          {
            this.acceptsAll(Lists.newArrayList("config"), "")
                .withRequiredArg()
                .ofType(File.class)
                .defaultsTo(new File("server.properties"));
            this.acceptsAll(Lists.newArrayList("sportpaper-settings"), "")
                .withRequiredArg()
                .ofType(File.class)
                .defaultsTo(new File("sportpaper.yml"));
            this.acceptsAll(Lists.newArrayList("plugins"), "")
                .withRequiredArg()
                .ofType(File.class)
                .defaultsTo(new File("plugins"));
          }
        }.parse());
    this.plugins = plugins;
    this.name = super.getServerModName()
        + " (with "
        + Stream.of(plugins).map(PluginDescriptionFile::getName).collect(Collectors.joining(", "))
        + ")";
  }

  @Override
  public void run() {
    if (!init) {
      init = true;
      primaryThread.start();
      return;
    }
    super.run();
  }

  @Override
  public String getServerModName() {
    return name;
  }

  @Override
  protected boolean init() throws IOException {
    logger = setupLogger();

    setupProperties();
    setupServer();
    setupPlugins();
    setupWorld();
    setupListener();
    setupConsole();
    setupProperties();

    return true;
  }

  protected Logger setupLogger() {
    AnsiConsole.systemInstall();

    final java.util.logging.Logger global = java.util.logging.Logger.getLogger("");
    global.setUseParentHandlers(false);

    final Handler[] handlers = global.getHandlers();
    for (int i = 0; i < handlers.length; ++i) {
      global.removeHandler(handlers[i]);
    }

    global.addHandler(new ForwardLogHandler());
    org.apache.logging.log4j.Logger logger = LogManager.getRootLogger();

    if (logger instanceof org.apache.logging.log4j.core.Logger) {
      final Iterator<org.apache.logging.log4j.core.Appender> appenders =
          ((org.apache.logging.log4j.core.Logger) logger)
              .getAppenders()
              .values()
              .iterator();
      while (appenders.hasNext()) {
        final org.apache.logging.log4j.core.Appender appender = appenders.next();
        if (appender instanceof ConsoleAppender) {
          ((org.apache.logging.log4j.core.Logger) logger).removeAppender(appender);
        }
      }
    }

    new Thread(new TerminalConsoleWriterThread(System.out, reader)).start();
    System.setOut(new PrintStream(new LoggerOutputStream(logger, Level.INFO), true));
    System.setErr(new PrintStream(new LoggerOutputStream(logger, Level.WARN), true));

    BasicConfigurator.resetConfiguration();

    var actualLogger = ReflectionUtils.readStaticField(
        DedicatedServer.class, org.apache.logging.log4j.Logger.class, "LOGGER");
    return actualLogger != null ? actualLogger : logger;
  }

  protected void setupConsole() {
    final Thread console = new Thread(() -> {
      try {
        while (!isStopped() && isRunning()) {
          final String command = reader.readLine(">", null);
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

  protected void setupProperties() {
    propertyManager = new EnvPropertyManager(options);

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
    setMotd(propertyManager.getString("motd", "A Minecraft 1.8 Server"));
    setWorld(propertyManager.getString("level-name", "world"));
    setPort(propertyManager.getInt("port", 25565));
    c(propertyManager.getInt("max-build-height", 256)); // setBuildHeight
    c(propertyManager.getString("server-ip", "0.0.0.0")); // setServerIp

    final String mode = propertyManager.getString("server-mode", "online");
    if (mode.equalsIgnoreCase("bungee")) {
      SpigotConfig.bungee = true;
    }
    final boolean online = mode.equalsIgnoreCase("online") || mode.equalsIgnoreCase("true");
    propertyManager.setProperty("online-mode", online);
    setOnlineMode(online);
  }

  protected void setupServer() {
    a(new DedicatedPlayerList(this));
    a(MinecraftEncryption.b());
    DispenserRegistry.c();
  }

  protected void setupPlugins() {
    server.loadPlugins();

    final RuntimePluginLoader loader = new RuntimePluginLoader(server);

    // TODO: Investigate why ViaRewind needs to be enabled explicitly
    final Plugin rewind = server.getPluginManager().getPlugin("ViaRewind");
    if (rewind != null) {
      loader.togglePlugin(rewind, true);
    }
    final Plugin backwards = server.getPluginManager().getPlugin("ViaBackwards");
    if (backwards != null) {
      loader.togglePlugin(backwards, true);
    }

    for (PluginDescriptionFile plugin : plugins) {
      loader.loadPlugin(plugin);
    }
    server.enablePlugins(PluginLoadOrder.POSTWORLD);
  }

  protected void setupWorld() {
    convertable = new WorldLoaderServer(server.getWorldContainer());

    final long start = System.nanoTime();
    a(U(), U(), 0 /* seed */, WorldType.FLAT, "" /* generator */);
    final long duration = System.nanoTime() - start;

    logger.info("Done (%.3fs)! For help, type \"help\" or \"?\"", (double) duration / 1.0E9D);
  }

  protected void setupListener() throws IOException {
    aq().a(InetAddress.getByName(getServerIp()), R());
  }
}
