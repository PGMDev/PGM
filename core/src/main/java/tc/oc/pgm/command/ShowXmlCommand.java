package tc.oc.pgm.command;

import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.noPermission;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.awt.Desktop;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;

/**
 * Opens-up the xml file on the server side, useful if you're running a server locally for mapdev
 * purposes on your machine. <br>
 * If the server isn't running in a desktop environment this command won't even be registered.
 */
@SuppressWarnings("UnstableApiUsage")
public final class ShowXmlCommand {

  private static final ShowXmlCommand INSTANCE = new ShowXmlCommand();

  private final boolean mayEnable = Desktop.isDesktopSupported();
  private byte[] serverIp = null;
  private String serverHostname = null;

  public ShowXmlCommand() {
    if (mayEnable) {
      new ServerIpResolver();
    }
  }

  public static ShowXmlCommand getInstance() {
    return INSTANCE;
  }

  public static boolean isEnabled() {
    return INSTANCE.mayEnable;
  }

  public static boolean isEnabledFor(CommandSender sender) {
    return INSTANCE.canUseCommand(sender);
  }

  @Command("showxml <map>")
  @CommandDescription("Show info about a map")
  @Permission("*")
  public void openXml(CommandSender sender, @Argument("map") @Greedy MapInfo map) {
    if (!canUseCommand(sender)) throw noPermission();

    try {
      Desktop.getDesktop().open(map.getSource().getAbsoluteXml().toFile());
    } catch (IOException e) {
      throw exception(e.getMessage());
    }
  }

  public boolean canUseCommand(CommandSender sender) {
    if (sender instanceof Player && sender.isOp()) {
      InetAddress addr = ((Player) sender).getAddress().getAddress();
      return addr.isLoopbackAddress()
          || (serverIp != null
              ? Arrays.equals(serverIp, addr.getAddress())
              : serverHostname != null && serverHostname.equals(addr.getCanonicalHostName()));
    }
    return sender instanceof ConsoleCommandSender;
  }

  private class ServerIpResolver implements Listener, PluginMessageListener {
    private static final String BUNGEE = "BungeeCord";
    private final Server server;
    private final Messenger messenger;

    private boolean unregistering;
    private boolean unregistered;
    private String serverName;

    ServerIpResolver() {
      this.server = Bukkit.getServer();
      this.messenger = server.getMessenger();

      messenger.registerOutgoingPluginChannel(PGM.get(), BUNGEE);
      messenger.registerIncomingPluginChannel(PGM.get(), BUNGEE, this);
      server.getPluginManager().registerEvents(this, PGM.get());
    }

    private void unregister() {
      unregistering = true;
      if (!unregistered) {
        messenger.unregisterOutgoingPluginChannel(PGM.get(), BUNGEE);
        messenger.unregisterIncomingPluginChannel(PGM.get(), BUNGEE, this);
        HandlerList.unregisterAll(this);
        unregistered = true;
      }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
      server.getScheduler().runTaskLater(PGM.get(), () -> usePlayer(e.getPlayer()), 25L);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
      if (!channel.equals(BUNGEE)) return;

      ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
      String subchannel = in.readUTF();
      if ("GetServer".equals(subchannel)) {
        this.serverName = in.readUTF();
        usePlayer(player);
      } else if ("ServerIP".equals(subchannel)) {
        in.readUTF(); // discard name
        serverHostname = in.readUTF(); // read ip
        in.readShort(); // discard port

        try {
          // Get actual address if possible
          serverIp = InetAddress.getByName(serverHostname).getAddress();
        } catch (UnknownHostException ignore) {
        }
        unregister();
      }
    }

    private void usePlayer(Player player) {
      if (!player.isOnline()) return;

      ByteArrayDataOutput out = ByteStreams.newDataOutput();
      if (serverName == null) {
        out.writeUTF("GetServer");
      } else if (serverIp == null) {
        out.writeUTF("ServerIP");
        out.writeUTF(serverName);
      } else {
        return;
      }

      player.sendPluginMessage(PGM.get(), BUNGEE, out.toByteArray());

      // If no response occurs in 30s from sending, assume not running under bungee and unregister
      if (!unregistering) {
        unregistering = true;
        server.getScheduler().scheduleSyncDelayedTask(PGM.get(), this::unregister, 30 * 20L);
      }
    }
  }
}
