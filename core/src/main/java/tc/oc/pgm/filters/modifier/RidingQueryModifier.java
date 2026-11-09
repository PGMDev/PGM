package tc.oc.pgm.filters.modifier;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.EntityQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Re-scopes a player query to whatever the player is riding (boat, minecart, horse...), so any
 * entity filter can be applied to it. Denies when the player isn't riding anything.
 */
public class RidingQueryModifier extends QueryModifier<PlayerQuery, EntityQuery> {

  public RidingQueryModifier(Filter filter) {
    super(filter, PlayerQuery.class, EntityQuery.class);
  }

  @Nullable
  @Override
  protected EntityQuery transformQuery(PlayerQuery query) {
    MatchPlayer player = query.getPlayer();
    if (player == null) return null;
    Entity vehicle = player.getBukkit().getVehicle();
    if (vehicle == null) return null;
    return new tc.oc.pgm.filters.query.EntityQuery(query.getEvent(), vehicle);
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableSet.<Class<? extends Event>>builder()
        .addAll(super.getRelevantEvents())
        .add(VehicleEnterEvent.class, VehicleExitEvent.class)
        .build();
  }
}
