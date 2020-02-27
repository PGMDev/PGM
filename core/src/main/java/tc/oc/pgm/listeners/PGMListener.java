package tc.oc.pgm.listeners;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.commands.MatchCommands;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.gamerules.GameRule;
import tc.oc.pgm.gamerules.GameRulesMatchModule;
import tc.oc.pgm.modules.TimeLockModule;
import tc.oc.util.bukkit.chat.NullCommandSender;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedText;
import tc.oc.util.bukkit.component.types.PersonalizedTranslatable;
import tc.oc.util.bukkit.translations.AllTranslations;

public class PGMListener implements Listener {
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
      final Server server = parent.getServer();
      if (server.isSuspended()) {
        server.setSuspended(false);
      }

      try {
        mm.createMatch(null).get();
      } catch (InterruptedException | ExecutionException e) {
        // No-op
      } finally {
        lock.writeLock().unlock();
      }
    }

    // If a match is being created, wait until its done
    try {
      lock.readLock().tryLock(15, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      // No-op
    } finally {
      lock.readLock().unlock();
    }

    if (!mm.getMatches().hasNext()) {
      event.disallow(
          AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
          AllTranslations.get()
              .translate("incorrectWorld.kickMessage", NullCommandSender.INSTANCE));
    }
  }

  @EventHandler
  public void onPlayerLogin(final PlayerLoginEvent event) {
    // allow premiums to join when the server is full
    if (event.getResult() == Result.KICK_FULL) {
      if (event.getPlayer().hasPermission(Permissions.JOIN_FULL)) {
        event.allow();
      } else {
        event.setKickMessage(AllTranslations.get().translate("serverFull", event.getPlayer()));
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW)
  public void addPlayerOnJoin(final PlayerJoinEvent event) {
    if (this.mm.getMatch(event.getWorld()) == null) {
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

    this.mm.getMatch(event.getWorld()).addPlayer(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void broadcastJoinMessage(final PlayerJoinEvent event) {
    // Handle join message and send it to all players except the one joining
    Match match = this.mm.getMatch(event.getWorld());
    if (match == null) return;

    if (event.getJoinMessage() != null) {
      event.setJoinMessage(null);
      MatchPlayer player = match.getPlayer(event.getPlayer());
      if (player != null) {
        announceJoinOrLeave(player, "broadcast.joinMessage");
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void removePlayerOnDisconnect(PlayerQuitEvent event) {
    Match match = this.mm.getMatch(event.getWorld());
    if (match == null) return;

    if (event.getQuitMessage() != null) {
      MatchPlayer player = match.getPlayer(event.getPlayer());
      if (player != null) {
        announceJoinOrLeave(player, "broadcast.leaveMessage");
      }
      event.setQuitMessage(null);
    }

    match.removePlayer(event.getPlayer());
    PGM.get().getPrefixRegistry().removePlayer(event.getPlayer().getUniqueId());
  }

  private void announceJoinOrLeave(MatchPlayer player, String messageKey) {
    checkNotNull(player);
    checkNotNull(messageKey);

    for (MatchPlayer viewer : player.getMatch().getPlayers()) {
      if (player.equals(viewer)) continue;

      SettingValue option = viewer.getSettings().getValue(SettingKey.JOIN);
      if (option.equals(SettingValue.JOIN_ON)) {
        String name = player.getBukkit().getDisplayName(viewer.getBukkit()) + ChatColor.YELLOW;
        Component component = new PersonalizedTranslatable(messageKey, name);
        viewer.sendMessage(new PersonalizedText(component, ChatColor.YELLOW));
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void sendWelcomeMessage(final PlayerJoinMatchEvent event) {
    final Match match = event.getMatch();
    final UUID viewerId = event.getPlayer().getId();
    match
        .getScheduler(MatchScope.LOADED)
        .runTaskLater(
            5L,
            new Runnable() {
              @Override
              public void run() {
                MatchPlayer viewer = match.getPlayer(viewerId);
                if (viewer == null) return;

                // FIXME: welcome message
                // match.sendWelcomeMessage(viewer);
              }
            });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void matchInfoOnParticipate(final PlayerPartyChangeEvent event) {
    if (event.getNewParty() instanceof Competitor) {
      MatchCommands.matchInfo(event.getPlayer().getBukkit(), event.getPlayer(), event.getMatch());
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
  public void unlockTime(final MatchStartEvent event) {
    boolean unlockTime = false;
    if (!event.getMatch().getModule(TimeLockModule.class).isTimeLocked()) {
      unlockTime = true;
    }

    GameRulesMatchModule gameRulesModule = event.getMatch().getModule(GameRulesMatchModule.class);
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
  public void lockTime(final MatchFinishEvent event) {
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

  @EventHandler(priority = EventPriority.MONITOR)
  public void dropItemsOnQuit(PlayerParticipationStopEvent event) {
    MatchPlayer quitter = event.getPlayer();
    if (!quitter.isAlive()) return;

    for (ItemStack item : quitter.getInventory().getContents()) {
      if (item == null || item.getType() == Material.AIR) continue;
      quitter.getWorld().dropItemNaturally(quitter.getBukkit().getLocation(), item);
    }

    for (ItemStack armor : quitter.getInventory().getArmorContents()) {
      if (armor == null || armor.getType() == Material.AIR) continue;
      quitter.getWorld().dropItemNaturally(quitter.getBukkit().getLocation(), armor);
    }
  }
}
