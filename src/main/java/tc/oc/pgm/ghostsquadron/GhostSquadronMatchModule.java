package tc.oc.pgm.ghostsquadron;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.ghostsquadron.RevealTask.RevealEntry;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.util.MatchPlayers;

@ListenerScope(MatchScope.RUNNING)
public class GhostSquadronMatchModule extends MatchModule implements Listener {
  BukkitTask mainTask;
  BukkitTask revealTask;
  final ClassMatchModule classMatchModule;
  public final Map<Location, UUID> landmines = Maps.newHashMap();
  public final Map<Location, Competitor> landmineTeams = Maps.newHashMap();
  public final Map<UUID, Date> spideySenses = Maps.newHashMap();
  final Map<Player, Double> walkDistance = Maps.newHashMap();

  public final Map<Player, RevealEntry> revealMap = Maps.newHashMap();

  // classes
  final @Nullable PlayerClass trackerClass;
  final @Nullable PlayerClass spiderClass;
  final @Nullable PlayerClass leprechaunClass;
  final @Nullable PlayerClass demoClass;
  // final PlayerClass ninjaClass;

  public GhostSquadronMatchModule(Match match, ClassMatchModule classMatchModule) {
    super(match);
    this.classMatchModule = checkNotNull(classMatchModule, "class match module");
    this.trackerClass = classMatchModule.getPlayerClass("Tracker");
    this.spiderClass = classMatchModule.getPlayerClass("Spider");
    this.leprechaunClass = classMatchModule.getPlayerClass("Leprechaun");
    this.demoClass = classMatchModule.getPlayerClass("Demo");
    // this.ninjaClass = checkNotNull(classMatchModule.getPlayerClass("Ninja"), "Ninja class not
    // found");
  }

  @Override
  public void enable() {
    GhostSquadronTask task = new GhostSquadronTask(this.match, this, this.classMatchModule);
    this.mainTask = Bukkit.getScheduler().runTaskTimer(PGM.get(), task, 0, 10);
    this.revealTask = Bukkit.getScheduler().runTaskTimer(PGM.get(), new RevealTask(this), 0, 1);
  }

  @Override
  public void disable() {
    this.mainTask.cancel();
    this.revealTask.cancel();
  }

  /*
   * GENERAL
   */
  @EventHandler
  public void cancelDrop(final PlayerDropItemEvent event) {
    Material hand = event.getPlayer().getItemInHand().getType();
    if (!GhostSquadron.ALLOWED_DROPS.contains(hand)) {
      MatchPlayer player = this.match.getPlayer(event.getPlayer());
      if (MatchPlayers.canInteract(player)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void cancelItemSpawn(final ItemSpawnEvent event) {
    Material hand = event.getEntity().getItemStack().getType();
    if (!GhostSquadron.ALLOWED_DROPS.contains(hand)) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void cancelPickup(final PlayerPickupItemEvent event) {
    event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void noExplosionBlockDamage(EntityExplodeEvent event) {
    event.blockList().clear();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void enforceFireTickLimit(EntityDamageEvent event) {
    event
        .getEntity()
        .setFireTicks(Math.min(event.getEntity().getFireTicks(), GhostSquadron.MAX_FIRE_TICKS));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void resetRevealTicks(PlayerDeathEvent event) {
    this.revealMap.remove(event.getEntity());
  }

  private void reveal(Player player) {
    this.reveal(player, GhostSquadron.REVEAL_STANDARD_DURATION);
  }

  private void reveal(Player player, int ticks) {
    RevealEntry entry = this.revealMap.get(player);
    if (entry == null) entry = new RevealEntry();

    entry.revealTicks = ticks;

    for (PotionEffect e : player.getActivePotionEffects()) {
      if (e.getType().equals(PotionEffectType.INVISIBILITY)) {
        entry.potionTicks = e.getDuration();
      }
    }

    player.removePotionEffect(PotionEffectType.INVISIBILITY);

    this.revealMap.put(player, entry);
  }

  /*
   * ARCHER
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void revealOnArrow(final EntityDamageByEntityEvent event) {
    if (event.getCause() == DamageCause.PROJECTILE
        && event.getDamager() instanceof Arrow
        && event.getEntity() instanceof Player) {
      this.reveal((Player) event.getEntity(), GhostSquadron.ARROW_REVEAL_DURATION);
    }
  }

  /*
   * TRACKER
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void trackerMove(final PlayerMoveEvent event) {
    MatchPlayer enemy = this.getMatch().getPlayer(event.getPlayer());
    if (!MatchPlayers.canInteract(enemy)) return;

    ImmutableList.builder();

    double distance = event.getFrom().distance(event.getTo());

    final Double walkedRaw = this.walkDistance.get(event.getPlayer());
    final double walkedStart = walkedRaw != null ? walkedRaw.doubleValue() : 0;
    final int stepStart = (int) Math.floor(walkedStart / GhostSquadron.TRACKER_FOOTSTEP_SPACING);

    final double walkedEnd = walkedStart + distance;
    final int stepEnd = (int) Math.floor(walkedEnd / GhostSquadron.TRACKER_FOOTSTEP_SPACING);

    this.walkDistance.put(event.getPlayer(), walkedEnd);

    Location normal = event.getTo().clone().subtract(event.getFrom());
    normal.multiply(1.0 / normal.length());

    for (int step = stepStart; step < stepEnd; step++) {
      double distanceFromStart = (step + 1) * GhostSquadron.TRACKER_FOOTSTEP_SPACING - walkedStart;
      Location stepLoc =
          normal
              .clone()
              .multiply(distanceFromStart)
              .add(event.getFrom())
              .add(0, GhostSquadron.TRACKER_FOOTSTEP_DY, 0);

      if (this.trackerClass != null) {
        for (UUID userId : this.classMatchModule.getClassMembers(this.trackerClass)) {
          MatchPlayer tracker = this.match.getPlayer(userId);
          if (MatchPlayers.canInteract(tracker) && tracker.getParty() != enemy.getParty()) {
            tracker.getBukkit().playEffect(stepLoc, Effect.FOOTSTEP, null);
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void clearWalkDistanceOnDeath(PlayerDeathEvent event) {
    // players are killed when leaving team or quitting, so this covers every instance
    this.walkDistance.remove(event.getEntity());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void trackerMelee(final EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
      MatchPlayer damager = this.getMatch().getPlayer((Player) event.getDamager());
      MatchPlayer damaged = this.getMatch().getPlayer((Player) event.getEntity());

      if (damager != null
          && damaged != null
          && this.isClass(damager, this.trackerClass)
          && damager.getParty() != damaged.getParty()) {
        ItemStack hand = damager.getBukkit().getItemInHand();
        if (hand != null && hand.getType() == Material.COMPASS) {
          this.reveal(damaged.getBukkit(), GhostSquadron.TRACKER_REVEAL_DURATION);
        }
      }
    }
  }

  /*
   * LEPRECHAUN
   */
  @EventHandler
  public void dontPickupExp(final PlayerPickupExperienceEvent event) {
    event.setCancelled(true);
  }

  @EventHandler
  public void fastLiquids(final PlayerMoveEvent event) {
    final MatchPlayer player = this.getMatch().getParticipant(event.getPlayer());
    if (player != null && this.isClass(player, this.leprechaunClass)) {
      if (event.getTo().getBlock().isLiquid()) {
        event.getPlayer().setAllowFlight(true);
        event.getPlayer().setFlying(true);
      } else {
        event.getPlayer().setFlying(false);
      }
    }
  }

  /*
   * DEMO
   */
  @EventHandler
  public void landminePlace(final PlayerInteractEvent event) {
    Player player = event.getPlayer();
    MatchPlayer mPlayer = match.getPlayer(player);
    if (!MatchPlayers.canInteract(mPlayer) || !this.isClass(mPlayer, this.demoClass)) return;

    final ItemStack item = event.getPlayer().getItemInHand();
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK
        && item != null
        && item.getType() == Material.TNT) {
      if (event.getClickedBlock().getRelative(BlockFace.UP).getType() != Material.AIR) {
        mPlayer.sendWarning(
            ChatColor.RED
                + AllTranslations.get()
                    .translate("match.ghostSquadron.landmine.invalidLocation", player),
            true);
        return;
      }

      if (this.landmines.containsKey(event.getClickedBlock().getLocation())) {
        mPlayer.sendWarning(
            ChatColor.RED
                + AllTranslations.get()
                    .translate("match.ghostSquadron.landmine.alreadyExists", player),
            true);
        return;
      }

      Location place = event.getClickedBlock().getLocation().add(.5, 0, .5);
      for (Location loc : this.landmines.keySet()) {
        boolean xClose = Math.abs(place.getX() - loc.getX()) <= GhostSquadron.LANDMINE_SPACING;
        boolean zClose = Math.abs(place.getZ() - loc.getZ()) <= GhostSquadron.LANDMINE_SPACING;
        if (xClose && zClose) {
          mPlayer.sendWarning(
              ChatColor.RED
                  + AllTranslations.get()
                      .translate("match.ghostSquadron.landmine.tooClose", player),
              true);
          return;
        }
      }

      event.setCancelled(true);
      this.landmines.put(place, mPlayer.getId());
      this.landmineTeams.put(place, mPlayer.getCompetitor());

      if (item.getAmount() > 1) {
        item.setAmount(item.getAmount() - 1);
      } else {
        event.getPlayer().setItemInHand(null);
      }

      player.sendMessage(
          ChatColor.GREEN
              + AllTranslations.get().translate("ghostSquadron.landminePlanted", player));
    }
  }

  @EventHandler
  public void landmineExplode(final PlayerMoveEvent event) {
    MatchPlayer player = this.getMatch().getPlayer(event.getPlayer());
    if (!MatchPlayers.canInteract(player)) return;

    Location to = event.getTo();
    Iterator<Map.Entry<Location, UUID>> iterator = this.landmines.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry<Location, UUID> entry = iterator.next();
      Location landmine = entry.getKey();
      MatchPlayer placer = this.getMatch().getPlayer(entry.getValue());

      if (placer == null || !placer.isParticipating()) {
        iterator.remove();
        continue;
      }

      Competitor placerTeam = this.landmineTeams.get(landmine);
      if (placerTeam == player.getParty()) continue;

      if (to.distanceSquared(landmine) < GhostSquadron.LANDMINE_ACTIVATION_DISTANCE_SQ) {
        TNTPrimed tnt =
            (TNTPrimed)
                landmine
                    .getWorld()
                    .spawnEntity(landmine.clone().add(0, 1, 0), EntityType.PRIMED_TNT);
        tnt.setFuseTicks(0);
        tnt.setYield(1);

        this.reveal(player.getBukkit());
        getMatch().callEvent(new ExplosionPrimeByEntityEvent(tnt, placer.getBukkit()));
        iterator.remove();
        this.landmineTeams.remove(landmine);
      }
    }
  }

  /*
   * SPIDER
   */
  public void spideySense(final MatchPlayer player) {
    UUID userId = player.getId();

    Date when = this.spideySenses.get(userId);
    Date now = new Date();

    if (when == null || now.getTime() > when.getTime() + GhostSquadron.SPIDEY_SENSE_COOLDOWN) {
      this.spideySenses.put(userId, now);

      player
          .getBukkit()
          .addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 4 * 20, 0), true);
      player.getBukkit().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 4 * 20, 1), true);

      player.getBukkit().playSound(player.getBukkit().getLocation(), Sound.SPIDER_IDLE, 5, 0);
      player.getBukkit().playSound(player.getBukkit().getLocation(), Sound.SPIDER_IDLE, 5, 0.25f);
      player.getBukkit().playSound(player.getBukkit().getLocation(), Sound.SPIDER_IDLE, 5, 0.5f);
    }
  }

  @EventHandler
  public void webBow(final EntityShootBowEvent event) {
    if (!(event.getEntity() instanceof Player)) return;
    Player player = (Player) event.getEntity();
    if (!this.isClass(this.getMatch().getPlayer(player), spiderClass)) return;

    FallingBlock web =
        event
            .getEntity()
            .getWorld()
            .spawnFallingBlock(event.getProjectile().getLocation(), Material.WEB, (byte) 0);
    web.setDropItem(false);
    web.setVelocity(event.getProjectile().getVelocity());
    event.setProjectile(web);
  }

  @EventHandler
  public void webLand(final EntityChangeBlockEvent event) {
    if (!(event.getEntity() instanceof FallingBlock)) return;
    FallingBlock block = (FallingBlock) event.getEntity();
    if (block.getMaterial() != Material.WEB) return;

    event.getEntity().getLocation().getBlock().setType(Material.WEB);
  }

  /*
   * NINJA - Temporarily disabled
   */

  /*
  @EventHandler
  public void hookPlayer(final PlayerFishEvent event) {
      if(event.getState() == PlayerFishEvent.State.FISHING || event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT) return;

      MatchPlayer caster = this.match.getPlayer(event.getPlayer());
      if (!this.isClass(caster, this.ninjaClass)) return;

      Location center = event.getHook().getLocation();
      Vector pullTowards = event.getPlayer().getLocation().toVector();

      for(Player player : event.getPlayer().getWorld().getPlayers()) {
          if(player == event.getPlayer()) continue;
          if(player.getLocation().distance(center) > 5) continue;
          MatchPlayer matchPlayer = this.match.getPlayer(player);
          if(!matchPlayer.canInteract() || matchPlayer.getTeam() == caster.getTeam()) continue;

          Vector velocity = pullTowards.subtract(player.getLocation().toVector()).divide(new Vector(3, 3, 3));

          double MIN = -2;
          double MAX = 2;
          velocity.setX(clamp(MIN, MAX, velocity.getX()));
          velocity.setY(clamp(MIN, MAX, velocity.getY()));
          velocity.setZ(clamp(MIN, MAX, velocity.getZ()));

          player.setVelocity(velocity);
          this.reveal(player);
      }

      event.getHook().remove();
  }
  */

  private static double clamp(double min, double max, double def) {
    if (def < min) return min;
    if (def > max) return max;
    return def;
  }

  private boolean isClass(MatchPlayer player, Optional<PlayerClass> playerClass) {
    return playerClass.isPresent() && isClass(player, playerClass.get());
  }

  private boolean isClass(MatchPlayer player, PlayerClass playerClass) {
    return classMatchModule.getPlayingClass(player.getId()).equals(playerClass);
  }
}
