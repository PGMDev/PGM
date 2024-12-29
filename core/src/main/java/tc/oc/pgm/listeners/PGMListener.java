package tc.oc.pgm.listeners;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;
import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.map.GameRule;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.channels.ChatManager;
import tc.oc.pgm.events.MapPoolAdjustEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerLeavePartyEvent;
import tc.oc.pgm.gamerules.GameRulesMatchModule;
import tc.oc.pgm.modules.WorldTimeModule;
import tc.oc.pgm.util.bukkit.WorldBorders;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextTranslations;

public class PGMListener implements Listener {
  /*
  1000  /time set day
  6000  noon, sun is at its peak
  12610 dusk
  13000 /time set night
  14000
  18000 midnight, moon is at its peak
  */
  private static final long[] WORLD_TIMES = {1000, 6000, 12610, 13000, 14000, 18000};

  private final Plugin parent;
  private final MatchManager mm;

  // Single-write, multi-read lock used to create the first match
  private final ReentrantReadWriteLock lock;

  public PGMListener(Plugin parent, MatchManager mm) {
    this.parent = parent;
    this.mm = mm;
    this.lock = new ReentrantReadWriteLock();
  }

  @EventHandler(ignoreCancelled = true)
  public void onPrePlayerLogin(final AsyncPlayerPreLoginEvent event) {
    if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED
        || mm.getMatches().hasNext()) return;

    // Create the match when the first player joins
    if (lock.writeLock().tryLock()) {
      // If the server is suspended, need to release so match can be created
      NMS_HACKS.resumeServer();

      try {
        mm.createMatch(null).get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      } finally {
        lock.writeLock().unlock();
      }
    }

    // If a match is being created, wait until its done
    try {
      lock.readLock().tryLock(15, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      lock.readLock().unlock();
    }

    if (!mm.getMatches().hasNext()) {
      event.disallow(
          AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
          TextTranslations.translate("misc.incorrectWorld"));
    }
  }

  @EventHandler
  public void onPlayerLogin(final PlayerLoginEvent event) {
    // allow premiums to join when the server is full
    if (event.getResult() == Result.KICK_FULL) {
      if (event.getPlayer().hasPermission(Permissions.JOIN_FULL)) {
        event.allow();
      } else {
        event.setKickMessage(TextTranslations.translate("misc.serverFull", event.getPlayer()));
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW)
  public void addPlayerOnJoin(final PlayerJoinEvent event) {
    // Player already left. Because quit already happened, we must ignore the join.
    if (!event.getPlayer().isOnline()) return;

    Match match = this.mm.getMatch(event.getPlayer().getWorld());
    if (match == null) {
      event
          .getPlayer()
          .kickPlayer(
              ChatColor.RED + TextTranslations.translate("misc.incorrectWorld", event.getPlayer()));
      this.parent
          .getLogger()
          .info("Had to kick player "
              + event.getPlayer().getName()
              + " due to them spawning in the incorrect world");
      return;
    }

    match.addPlayer(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void removePlayerOnDisconnect(PlayerQuitEvent event) {
    MatchPlayer player = this.mm.getPlayer(event.getPlayer());
    if (player == null) return;

    player.getMatch().removePlayer(event.getPlayer());
  }

  @EventHandler(ignoreCancelled = true)
  public void protect36(final PlayerInteractEvent event) {
    if (event.getClickedBlock() != null) {
      if (event.getClickedBlock().getType() == Materials.MOVING_PISTON) {
        event.setCancelled(true);
      }
    }
  }

  // sometimes arrows stuck in players persist through deaths
  @EventHandler
  public void fixStuckArrows(final PlayerRespawnEvent event) {
    PLAYER_UTILS.clearArrowsInPlayer(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void clearActiveEnderPearls(final PlayerDeathEvent event) {
    for (EnderPearl entity : event.getEntity().getWorld().getEntitiesByClass(EnderPearl.class)) {
      if (entity.getShooter() == event.getEntity()) entity.remove();
    }
  }

  // fix item pickup to work the way it should
  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
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

  @EventHandler
  public void initGamerules(final MatchLoadEvent event) {
    setGameRule(event, GameRule.DO_FIRE_TICK.getId(), false);
  }

  @EventHandler
  public void unlockFireTick(final MatchStartEvent event) {
    event
        .getMatch()
        .getWorld()
        .setGameRuleValue(
            GameRule.DO_FIRE_TICK.getId(),
            event
                .getMatch()
                .needModule(GameRulesMatchModule.class)
                .getGameRule(GameRule.DO_FIRE_TICK.getId()));
  }

  @EventHandler
  public void postGamerules(final MatchFinishEvent event) {
    setGameRule(event, GameRule.DO_FIRE_TICK.getId(), false);
  }

  //
  // Time Lock
  // lock time before, during (if time lock enabled), and after the match
  //
  @EventHandler
  public void lockTime(final MatchLoadEvent event) {
    setGameRule(event, GameRule.DO_DAYLIGHT_CYCLE.getId(), false);
  }

  @EventHandler
  public void unlockTime(final MatchStartEvent event) {
    event
        .getMatch()
        .getWorld()
        .setGameRuleValue(
            GameRule.DO_DAYLIGHT_CYCLE.getId(),
            event
                .getMatch()
                .needModule(GameRulesMatchModule.class)
                .getGameRule(GameRule.DO_DAYLIGHT_CYCLE.getId()));
  }

  @EventHandler
  public void lockTime(final MatchFinishEvent event) {
    setGameRule(event, GameRule.DO_DAYLIGHT_CYCLE.getId(), false);
  }

  @EventHandler
  public void setTime(final MatchLoadEvent event) {
    Long time = event.getMatch().getModule(WorldTimeModule.class).getTime();
    if (time != null) {
      event.getMatch().getWorld().setTime(time);
    }
  }

  @EventHandler
  public void randomTime(final MatchLoadEvent event) {
    if (event.getMatch().getModule(WorldTimeModule.class).isTimeRandom()) {
      Random rand = event.getMatch().getRandom();
      long time = WORLD_TIMES[rand.nextInt(WORLD_TIMES.length)];
      event.getMatch().getWorld().setTime(time);
    }
  }

  @EventHandler
  public void freezeWorld(final BlockTransformEvent event) {
    Match match = this.mm.getMatch(event.getWorld());
    if (match == null || match.isFinished()) event.setCancelled(true);
  }

  @EventHandler
  public void freezeVehicle(final VehicleUpdateEvent event) {
    Match match = this.mm.getMatch(event.getVehicle().getWorld());
    if (match == null || match.isFinished()) {
      event.getVehicle().setVelocity(new Vector());
    }
  }

  @EventHandler
  public void nerfFishing(PlayerFishEvent event) {
    if (event.getCaught() instanceof Item) {
      Item caught = (Item) event.getCaught();
      if (caught.getItemStack().getType() != Materials.RAW_FISH) {
        caught.setItemStack(new ItemStack(Materials.RAW_FISH));
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void dropItemsOnQuit(PlayerLeavePartyEvent event) {
    MatchPlayer quitter = event.getPlayer();
    if (!quitter.isAlive()) return;

    for (ItemStack item : quitter.getInventory().getContents()) {
      if (item == null || item.getType() == Material.AIR) continue;
      quitter.getBukkit().getWorld().dropItemNaturally(quitter.getBukkit().getLocation(), item);
    }

    for (ItemStack armor : quitter.getInventory().getArmorContents()) {
      if (armor == null || armor.getType() == Material.AIR) continue;
      quitter.getBukkit().getWorld().dropItemNaturally(quitter.getBukkit().getLocation(), armor);
    }
  }

  @EventHandler
  public void announceDynamicMapPoolChange(MapPoolAdjustEvent event) {
    // Send feedback to staff, alerting them that the map pool has changed by force
    if (event.isForced()) {
      Component poolName = text(event.getNewPool().getName(), NamedTextColor.LIGHT_PURPLE);
      Component staffName = player(event.getSender());
      Component matchLimit = text()
          .append(text(event.getMatchLimit(), NamedTextColor.GREEN))
          .append(space())
          .append(translatable(
              "match.name" + (event.getMatchLimit() != 1 ? ".plural" : ""), NamedTextColor.GRAY))
          .build();

      // No limit
      Component forced = translatable("pool.change.force", poolName, staffName);
      if (event.getTimeLimit() != null) {
        Component time = TemporalComponent.briefNaturalApproximate(event.getTimeLimit())
            .color(NamedTextColor.GREEN);

        // If time & match limit are present, display both
        if (event.getMatchLimit() != 0) {
          Component timeAndLimit = translatable("misc.or", NamedTextColor.GRAY, time, matchLimit);
          forced = translatable("pool.change.forceTimed", poolName, timeAndLimit, staffName);
        } else {
          // Just time limit
          forced = translatable("pool.change.forceTimed", poolName, time, staffName);
        }
      } else if (event.getMatchLimit() != 0) {
        // Just match limit
        forced = translatable("pool.change.forceTimed", poolName, matchLimit, staffName);
      }

      ChatManager.broadcastAdminMessage(forced.color(NamedTextColor.GRAY));
    }

    // Broadcast map pool changes due to size
    if (event.getNewPool().isDynamic()) {
      Component broadcast = text()
          .append(text("[", NamedTextColor.WHITE))
          .append(translatable("pool.name", NamedTextColor.GOLD))
          .append(text("] ", NamedTextColor.WHITE))
          .append(translatable(
              "pool.change",
              NamedTextColor.GREEN,
              text(event.getNewPool().getName(), NamedTextColor.AQUA)))
          .build();

      event.getMatch().sendMessage(broadcast);
    }
  }

  @EventHandler // We only need to store skins for the post match stats
  public void storeSkinOnMatchJoin(PlayerJoinMatchEvent event) {
    final MatchPlayer player = event.getPlayer();
    Skin playerSkin = PLAYER_UTILS.getPlayerSkin(player.getBukkit());
    if (playerSkin != null) {
      PGM.get().getDatastore().setSkin(player.getId(), playerSkin);
    }
  }

  public void setGameRule(MatchLoadEvent event, String gameRule, boolean gameRuleValue) {
    event.getMatch().getWorld().setGameRuleValue(gameRule, Boolean.toString(gameRuleValue));
  }

  public void setGameRule(MatchFinishEvent event, String gameRule, boolean gameRuleValue) {
    event.getMatch().getWorld().setGameRuleValue(gameRule, Boolean.toString(gameRuleValue));
  }

  /** Prevent teleporting outside the border */
  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerTeleport(final PlayerTeleportEvent event) {
    if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
      if (WorldBorders.isInsideBorder(event.getFrom())
          && !WorldBorders.isInsideBorder(event.getTo())) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerMove(final PlayerCoarseMoveEvent event) {
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player != null && player.isObserving()) {
      Location location = event.getTo();
      if (WorldBorders.clampToBorder(location)) {
        event.setTo(location);
      }
    }
  }
}
