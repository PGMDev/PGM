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
import tc.oc.pgm.platform.modern.modules.waypoint.WaypointDefinition;
import tc.oc.pgm.platform.modern.modules.waypoint.WaypointMatchModule;
import tc.oc.pgm.platform.modern.modules.waypoint.types.DefinedWaypointTransmitter;

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
      var transmitter = new DefinedWaypointTransmitter(definition.getWaypoint().get(), mannequin);
      match.needModule(WaypointMatchModule.class).track(transmitter);
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
    instances.removeAll(id).forEach(mannequin -> {
      byEntity.remove(mannequin.getEntityId());
      mannequin.despawn();
    });
  }

  public void modify(String id, Consumer<Mannequin> modifier) {
    instances.get(id).forEach(modifier);
  }

  public void setWaypoint(Mannequin mannequin, @Nullable WaypointDefinition definition) {
    var wmm = match.needModule(WaypointMatchModule.class);
    var current = mannequin.getWaypointTransmitter();
    if (current != null) wmm.untrack(current);
    if (definition != null) {
      var transmitter = new DefinedWaypointTransmitter(definition, mannequin);
      wmm.track(transmitter);
      mannequin.setWaypointTransmitter(transmitter);
    } else {
      mannequin.setWaypointTransmitter(null);
    }
  }

  @EventHandler
  public void onDeath(EntityDeathEvent event) {
    Mannequin mannequin = byEntity.remove(event.getEntity().getUniqueId());
    if (mannequin == null) return;
    setWaypoint(mannequin, null);
    instances.remove(mannequin.getId(), mannequin);
  }
}
