package tc.oc.pgm.shield;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PotionEffectRemoveEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.match.*;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;

@ModuleDescription(name = "Shield")
@ListenerScope(MatchScope.LOADED)
public class ShieldMatchModule extends MatchModule implements Listener, Tickable {

  final Map<MatchPlayer, ShieldPlayerModule> playerShields = new HashMap<>();

  public ShieldMatchModule(Match match) {
    super(match);
  }

  ShieldPlayerModule getShield(Entity player) {
    MatchPlayer matchPlayer = getMatch().getPlayer(player);
    return matchPlayer == null ? null : playerShields.get(matchPlayer);
  }

  public void applyShield(MatchPlayer player, ShieldParameters parameters) {
    removeShield(player);
    if (parameters.maxHealth > 0) {
      ShieldPlayerModule shield = new ShieldPlayerModule(this, player, parameters);
      shield.apply();
      playerShields.put(player, shield);
    }
  }

  public void removeShield(MatchPlayer player) {
    ShieldPlayerModule shield = playerShields.remove(player);
    if (shield != null) shield.remove();
  }

  @Override
  public void tick(Match match) {
    for (ShieldPlayerModule shield : playerShields.values()) {
      shield.tick(match);
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
