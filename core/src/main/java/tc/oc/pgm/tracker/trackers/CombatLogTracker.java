package tc.oc.pgm.tracker.trackers;

import static net.kyori.adventure.text.Component.translatable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.util.material.Materials;

/**
 * Predicts the death of players who disconnect while participating, and simulates the damage and
 * death events that would have been fired if they had stayed in the game.
 *
 * <p>Also prevents team switching while in imminent danger..
 */
public class CombatLogTracker implements Listener {
  // Logout within this time since last damage is considered combat log
  private static final Duration RECENT_DAMAGE_THRESHOLD = Duration.ofSeconds(3);

  // Maximum height player can fall without taking damage
  private static final double SAFE_FALL_DISTANCE = 2;

  // Minimum water required to stop the player's fall
  private static final int BREAK_FALL_WATER_DEPTH = 3;

  private static class Damage {
    public final Instant time;
    public final EntityDamageEvent event;

    private Damage(Instant time, EntityDamageEvent event) {
      this.time = time;
      this.event = event;
    }
  }

  private static class ImminentDeath {
    public final EntityDamageEvent.DamageCause cause; // what will cause the death
    public final Location deathLocation;
    public final Block blockDamager;
    public final boolean alreadyDamaged; // if the player has already been damaged by this cause

    private ImminentDeath(
        EntityDamageEvent.DamageCause cause,
        Location deathLocation,
        @Nullable Block blockDamager,
        boolean damaged) {
      this.cause = cause;
      this.deathLocation = deathLocation;
      this.blockDamager = blockDamager;
      this.alreadyDamaged = damaged;
    }
  }

  private Map<Player, Damage> recentDamage = new HashMap<>();

  public CombatLogTracker(TrackerMatchModule tmm) {}

  private static boolean hasFireResistance(LivingEntity entity) {
    for (PotionEffect effect : entity.getActivePotionEffects()) {
      if (PotionEffectType.FIRE_RESISTANCE.equals(effect.getType())) return true;
    }
    return false;
  }

  private static double getResistanceFactor(LivingEntity entity) {
    int amplifier = 0;
    for (PotionEffect effect : entity.getActivePotionEffects()) {
      if (PotionEffectType.DAMAGE_RESISTANCE.equals(effect.getType())
          && effect.getAmplifier() > amplifier) {
        amplifier = effect.getAmplifier();
      }
    }
    return 1d - (amplifier / 5d);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDamage(EntityDamageEvent event) {
    if (event.getDamage() <= 0) return;

    if (!(event.getEntity() instanceof Player)) return;
    Player player = (Player) event.getEntity();

    if (player.getGameMode() == GameMode.CREATIVE) return;

    if (player.hasMetadata("isDead") || player.isDead()) return;

    if (player.getNoDamageTicks() > 0) return;

    if (getResistanceFactor(player) <= 0) return;

    switch (event.getCause()) {
      case ENTITY_EXPLOSION:
      case BLOCK_EXPLOSION:
      case CUSTOM:
      case FALL:
      case FALLING_BLOCK:
      case LIGHTNING:
      case MELTING:
      case SUICIDE:
      case THORNS:
        return; // Skip damage causes that are not particularly likely to be followed by more damage

      case FIRE:
      case FIRE_TICK:
      case LAVA:
        if (hasFireResistance(player)) return;
        break;
    }

    // Record the player's damage with a timestamp
    this.recentDamage.put(player, new Damage(Instant.now(), event));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    // Clear last damage when a player dies
    this.recentDamage.remove(event.getEntity());
  }

  // This must be called BEFORE the listener that removes the player from the match
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onQuit(PlayerQuitEvent event) {
    Match match = PGM.get().getMatchManager().getMatch(event.getPlayer().getWorld());
    if (match == null || !match.isRunning()) return;

    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null || !player.isParticipating()) return;

    ImminentDeath imminentDeath = this.getImminentDeath(player.getBukkit());
    if (imminentDeath == null) return;

    if (!imminentDeath.alreadyDamaged) {
      // Simulate the damage event that would have killed them,
      // allowing the tracker to figure out the cause of death
      EntityDamageEvent damageEvent;
      if (imminentDeath.blockDamager != null) {
        damageEvent =
            new EntityDamageByBlockEvent(
                imminentDeath.blockDamager,
                player.getBukkit(),
                imminentDeath.cause,
                player.getBukkit().getHealth());
      } else {
        damageEvent =
            new EntityDamageEvent(
                player.getBukkit(), imminentDeath.cause, player.getBukkit().getHealth());
      }
      match.callEvent(damageEvent);

      // If the damage event was cancelled, don't simulate the kill
      if (damageEvent.isCancelled()) return;

      player.getBukkit().setLastDamageCause(damageEvent);
    }

    // Simulate the player's death. The tracker will assume the death was caused by the
    // last damage event, which was either a real one or the fake one we generated above.
    ArrayList<ItemStack> drops = new ArrayList<>();
    for (ItemStack stack : player.getInventory().getContents()) {
      if (stack != null && stack.getType() != Material.AIR) drops.add(stack);
    }
    for (ItemStack stack : player.getInventory().getArmorContents()) {
      if (stack != null && stack.getType() != Material.AIR) drops.add(stack);
    }

    try {
      currentDeathEvent =
          new PlayerDeathEvent(
              player.getBukkit(),
              drops,
              0,
              player.getBukkit().getDisplayName() + " logged out to avoid death");
      match.callEvent(currentDeathEvent);
    } finally {
      currentDeathEvent = null;
    }
  }

  // A simple way to tag an event as a combat log, hacky but it works
  private static @Nullable PlayerDeathEvent currentDeathEvent;

  public static boolean isCombatLog(PlayerDeathEvent event) {
    return event == currentDeathEvent;
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onParticipationStop(PlayerParticipationStopEvent event) {
    if (event.getMatch().isRunning()
        && !event.getRequest().has(JoinRequest.Flag.FORCE)
        && this.getImminentDeath(event.getPlayer().getBukkit()) != null) {
      event.cancel(translatable("leave.err.combatLog"));
      event.setCancelled(true);
    }
  }

  /**
   * Get the cause of the player's imminent death, or null if they are not about to die NOTE: not
   * idempotent, has the side effect of clearing the recentDamage cache
   */
  public @Nullable ImminentDeath getImminentDeath(Player player) {
    // If the player is already dead or in creative mode, we don't care
    if (player.isDead()
        || player.hasMetadata("isDead")
        || player.getGameMode() == GameMode.CREATIVE) return null;

    // If the player was on the ground, or is flying, or is able to fly, they are fine
    if (!(player.isOnGround() || player.isFlying() || player.getAllowFlight())) {
      // If the player is falling, detect an imminent falling death
      double fallDistance = player.getFallDistance();
      Block landingBlock = null;
      int waterDepth = 0;
      Location location = player.getLocation();

      if (location.getY() > 256) {
        // If player is above Y 256, assume they fell at least to there
        fallDistance += location.getY() - 256;
        location.setY(256);
      }

      // Search the blocks directly beneath the player until we find what they would have landed on
      Block block = null;
      for (; location.getY() >= 0; location.add(0, -1, 0)) {
        block = location.getBlock();
        if (block != null) {
          landingBlock = block;

          if (Materials.isWater(landingBlock.getType())) {
            // If the player falls through water, reset fall distance and inc the water depth
            fallDistance = -1;
            waterDepth += 1;

            // Break if they have fallen through enough water to stop falling
            if (waterDepth >= BREAK_FALL_WATER_DEPTH) break;
          } else {
            // If the block is not water, reset the water depth
            waterDepth = 0;

            if (Materials.isSolid(landingBlock.getType())
                || Materials.isLava(landingBlock.getType())) {
              // Break if the player hits a solid block or lava
              break;
            } else if (landingBlock.getType() == Material.WEB) {
              // If they hit web, reset their fall distance, but assume they keep falling
              fallDistance = -1;
            }
          }
        }

        fallDistance += 1;
      }

      double resistanceFactor = getResistanceFactor(player);
      boolean fireResistance = hasFireResistance(player);

      // Now decide if the landing would have killed them
      if (location.getBlockY() < 0) {
        // The player would have fallen into the void
        return new ImminentDeath(EntityDamageEvent.DamageCause.VOID, location, null, false);
      } else if (landingBlock != null) {
        if (Materials.isSolid(landingBlock.getType())
            && player.getHealth() <= resistanceFactor * (fallDistance - SAFE_FALL_DISTANCE)) {
          // The player would have landed on a solid block and taken enough fall damage to kill them
          return new ImminentDeath(
              EntityDamageEvent.DamageCause.FALL,
              landingBlock.getLocation().add(0, 0.5, 0),
              null,
              false);
        } else if (Materials.isLava(landingBlock.getType())
            && resistanceFactor > 0
            && !fireResistance) {
          // The player would have landed in lava, and we give the lava the benefit of the doubt
          return new ImminentDeath(
              EntityDamageEvent.DamageCause.LAVA, landingBlock.getLocation(), landingBlock, false);
        }
      }
    }

    // If we didn't predict a falling death, detect combat log due to recent damage
    Damage damage = this.recentDamage.remove(player);
    if (damage != null && damage.time.plus(RECENT_DAMAGE_THRESHOLD).isAfter(Instant.now())) {
      // Player logged out too soon after taking damage
      return new ImminentDeath(damage.event.getCause(), player.getLocation(), null, true);
    }

    return null;
  }
}
