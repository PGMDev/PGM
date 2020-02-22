package tc.oc.pgm.shield;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PotionEffectRemoveEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.util.ClassLogger;

@ListenerScope(MatchScope.LOADED)
public class ShieldMatchModule implements MatchModule, Listener, Tickable {

  final Map<MatchPlayer, ShieldPlayerModule> playerShields = new HashMap<>();
  final Match match;
  final Logger logger;

  public ShieldMatchModule(Match match) {
    this.match = match;
    this.logger = ClassLogger.get(match.getLogger(), getClass());
  }

  ShieldPlayerModule getShield(Entity player) {
    MatchPlayer matchPlayer = match.getPlayer(player);
    return matchPlayer == null ? null : playerShields.get(matchPlayer);
  }

  public void applyShield(MatchPlayer player, ShieldParameters parameters) {
    removeShield(player);
    if (parameters.maxHealth > 0) {
      ShieldPlayerModule shield = new ShieldPlayerModule(logger, player, parameters);
      shield.apply();
      playerShields.put(player, shield);
    }
  }

  public void removeShield(MatchPlayer player) {
    ShieldPlayerModule shield = playerShields.remove(player);
    if (shield != null) shield.remove();
  }

  @Override
  public void tick(Match match, Tick tick) {
    for (ShieldPlayerModule shield : playerShields.values()) {
      shield.tick(match, tick);
    }
  }

  @EventHandler
  public void onDespawn(ParticipantDespawnEvent event) {
    playerShields.remove(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    ShieldPlayerModule shield = getShield(event.getEntity());
    if (shield != null) shield.onEvent(event);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPotionRemove(PotionEffectRemoveEvent event) {
    ShieldPlayerModule shield = getShield(event.getEntity());
    if (shield != null) shield.onEvent(event);
  }
}
