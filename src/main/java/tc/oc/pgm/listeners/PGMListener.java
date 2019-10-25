package tc.oc.pgm.listeners;

import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.Config;
import tc.oc.pgm.events.*;
import tc.oc.pgm.gamerules.GameRule;
import tc.oc.pgm.gamerules.GameRulesModule;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.match.*;
import tc.oc.pgm.modules.TimeLockModule;
import tc.oc.server.Permissions;

public class PGMListener implements Listener {
  private final Plugin parent;
  private final MatchManager mm;

  public PGMListener(Plugin parent, MatchManager mm) {
    this.parent = parent;
    this.mm = mm;
  }

  @EventHandler
  public void onServerListPing(ServerListPingEvent event) {
    final Match match = mm.getCurrentMatch();
    if (match != null) {
      final PGMMap map = match.getMap();
      if (map != null) {
        final ChatColor color = match.isRunning() ? ChatColor.GREEN : ChatColor.YELLOW;
        event.setMotd(color + "\u00BB " + ChatColor.AQUA + map.getName() + color + " \u00AB");
      }
    }
  }

  @EventHandler
  public void onPlayerLogin(final PlayerLoginEvent event) {
    // allow ops to join when the server is full
    if (event.getResult() == Result.KICK_FULL) {
      if (event.getPlayer().hasPermission(Permissions.JOIN_FULL)) {
        event.allow();
      } else {
        event.setKickMessage(
            AllTranslations.get()
                .translate("serverFull", mm.getPlayer(event.getPlayer()).getBukkit()));
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW)
  public void addPlayerOnJoin(final PlayerJoinEvent event) {
    if (event.getPlayer().getLocation().getWorld() != this.mm.getCurrentMatch().getWorld()) {
      event
          .getPlayer()
          .kickPlayer(
              ChatColor.RED
                  + AllTranslations.get()
                      .translate("incorrectWorld.kickMessage", event.getPlayer()));
      this.parent
          .getLogger()
          .info(
              "Had to kick player "
                  + event.getPlayer().getName()
                  + " due to them spawning in the incorrect world");
      return;
    }

    this.mm.getCurrentMatch().addPlayer(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void broadcastJoinMessage(final PlayerJoinEvent event) {
    // Handle join message and send it to all players except the one joining
    if (event.getJoinMessage() != null) {
      event.setJoinMessage(null);
      Match match = this.mm.getCurrentMatch();
      MatchPlayer player = match.getPlayer(event.getPlayer());
      if (player != null) {
        for (MatchPlayer viewer : match.getPlayers()) {
          if (!player.equals(viewer)) {
            viewer.sendMessage(
                ChatColor.YELLOW
                    + AllTranslations.get()
                        .translate(
                            "broadcast.joinMessage",
                            viewer.getBukkit(),
                            player.getColoredName(viewer) + ChatColor.YELLOW));
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void removePlayerOnDisconnect(PlayerQuitEvent event) {
    Match match = this.mm.getCurrentMatch();
    if (match == null) return;

    if (event.getQuitMessage() != null) {
      MatchPlayer player = match.getPlayer(event.getPlayer());
      if (player != null) {
        for (MatchPlayer viewer : match.getPlayers()) {
          if (!player.equals(viewer)) {
            viewer.sendMessage(
                ChatColor.YELLOW
                    + AllTranslations.get()
                        .translate(
                            "broadcast.leaveMessage",
                            viewer.getBukkit(),
                            player.getColoredName(viewer) + ChatColor.YELLOW));
          }
        }
      }
      event.setQuitMessage(null);
    }

    match.removePlayer(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void sendWelcomeMessage(final PlayerJoinMatchEvent event) {
    final Match match = event.getMatch();
    final UUID viewerId = event.getPlayer().getPlayerId();
    match
        .getScheduler(MatchScope.LOADED)
        .runTaskLater(
            5L,
            new Runnable() {
              @Override
              public void run() {
                MatchPlayer viewer = match.getPlayer(viewerId);
                if (viewer == null) return;

                match.sendWelcomeMessage(viewer);
              }
            });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void matchInfoOnParticipate(final PlayerPartyChangeEvent event) {
    if (event.getNewParty() instanceof Competitor) {
      // MatchCommands.sendMatchInfo(event.getPlayer().getBukkit(), event.getMatch());
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void protect36(final PlayerInteractEvent event) {
    if (event.getClickedBlock() != null) {
      if (event.getClickedBlock().getType() == Material.PISTON_MOVING_PIECE) {
        event.setCancelled(true);
      }
    }
  }

  // sometimes arrows stuck in players persist through deaths
  @EventHandler
  public void fixStuckArrows(final PlayerRespawnEvent event) {
    event.getPlayer().setArrowsStuck(0);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void clearActiveEnderPearls(final PlayerDeathEvent event) {
    for (Entity entity : event.getEntity().getWorld().getEntitiesByClass(EnderPearl.class)) {
      if (((EnderPearl) entity).getShooter() == event.getEntity()) {
        entity.remove();
      }
    }
  }

  // fix item pickup to work the way it should
  @EventHandler(priority = EventPriority.HIGHEST)
  public void handleItemPickup(final PlayerPickupItemEvent event) {
    Player nearestPlayer = event.getPlayer();
    double closestDistance =
        event.getItem().getLocation().distance(event.getPlayer().getLocation());

    for (Entity nearEntity : event.getItem().getNearbyEntities(1.5, 1.5, 1.5)) {
      double distance = event.getItem().getLocation().distanceSquared(nearEntity.getLocation());

      if (nearEntity instanceof Player && distance < closestDistance) {
        nearestPlayer = (Player) nearEntity;
        closestDistance = distance;
      }
    }

    if (nearestPlayer != event.getPlayer()) event.setCancelled(true);
  }

  //
  // Time Lock
  // lock time before, during (if time lock enabled), and after the match
  //
  @EventHandler
  public void lockTime(final MatchLoadEvent event) {
    event
        .getMatch()
        .getWorld()
        .setGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE.getValue(), Boolean.toString(false));
  }

  @EventHandler
  public void unlockTime(final MatchBeginEvent event) {
    boolean unlockTime = false;
    if (!event.getMatch().getModuleContext().getModule(TimeLockModule.class).isTimeLocked()) {
      unlockTime = true;
    }

    GameRulesModule gameRulesModule =
        event.getMatch().getModuleContext().getModule(GameRulesModule.class);

    if (gameRulesModule != null
        && gameRulesModule.getGameRules().containsKey(GameRule.DO_DAYLIGHT_CYCLE)) {
      unlockTime = gameRulesModule.getGameRules().get(GameRule.DO_DAYLIGHT_CYCLE);
    }

    event
        .getMatch()
        .getWorld()
        .setGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE.getValue(), Boolean.toString(unlockTime));
  }

  @EventHandler
  public void lockTime(final MatchEndEvent event) {
    event
        .getMatch()
        .getWorld()
        .setGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE.getValue(), Boolean.toString(false));
  }

  @EventHandler
  public void nerfFishing(PlayerFishEvent event) {
    if (Config.Fishing.disableTreasure() && event.getCaught() instanceof Item) {
      Item caught = (Item) event.getCaught();
      if (caught.getItemStack().getType() != Material.RAW_FISH) {
        caught.setItemStack(new ItemStack(Material.RAW_FISH));
      }
    }
  }
}
