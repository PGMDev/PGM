package tc.oc.pgm.platform.modern.modules.mannequin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.platform.modern.modules.behavior.BehaviorMatchModule;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.looking.LookBehavior;
import tc.oc.pgm.platform.modern.modules.waypoint.WaypointDefinition;
import tc.oc.pgm.platform.modern.modules.waypoint.WaypointMatchModule;

@ListenerScope(MatchScope.RUNNING)
public class MannequinMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Map<String, MannequinDefinition> mannequinDefinitions;
  private final SetMultimap<String, Mannequin> instances = HashMultimap.create();
  private final Map<UUID, Mannequin> byEntity = new HashMap<>();

  public MannequinMatchModule(Match match, Map<String, MannequinDefinition> mannequinDefinitions) {
    this.match = match;
    this.mannequinDefinitions = mannequinDefinitions;
  }

  public void spawn(
      MannequinDefinition definition, double x, double y, double z, float yaw, float pitch) {
    Location origin = new Location(match.getWorld(), x, y, z, yaw, pitch);
    Mannequin mannequin = Mannequin.spawn(origin, definition);
    instances.put(definition.getId(), mannequin);
    byEntity.put(mannequin.getEntityId(), mannequin);

    if (definition.getWaypoint() != null) {
      setWaypoint(mannequin, definition.getWaypoint().get());
    }

    if (definition.getBehavior() != null) {
      match.needModule(BehaviorMatchModule.class).register(mannequin, definition.getBehavior().get());
    }
  }

  @EventHandler
  public void onInteract(PlayerInteractAtEntityEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) return;
    Mannequin mannequin = byEntity.get(event.getRightClicked().getUniqueId());
    if (mannequin == null) return;
    if (mannequin.getDefinition().getAction() == null) return;
    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null) return;
    event.setCancelled(true);
    mannequin.getDefinition().getAction().trigger(player);
  }

  public void despawn(String id) {
    var bmm = match.needModule(BehaviorMatchModule.class);
    instances.removeAll(id).forEach(mannequin -> {
      byEntity.remove(mannequin.getEntityId());
      bmm.unregister(mannequin);
      mannequin.despawn();
    });
  }

  public void modify(String id, Consumer<Mannequin> modifier) {
    instances.get(id).forEach(modifier);
  }

  public void setWaypoint(Mannequin mannequin, @Nullable WaypointDefinition definition) {
    match
        .needModule(WaypointMatchModule.class)
        .applyEntityWaypoint(mannequin.getEntity(), definition);
  }

  public void removeWaypoint(Mannequin mannequin) {
    setWaypoint(mannequin, null);
  }

  @EventHandler
  public void onDeath(EntityDeathEvent event) {
    Mannequin mannequin = byEntity.remove(event.getEntity().getUniqueId());
    if (mannequin == null) return;
    setWaypoint(mannequin, null);
    match.needModule(BehaviorMatchModule.class).unregister(mannequin);
    instances.remove(mannequin.getId(), mannequin);
  }
}
