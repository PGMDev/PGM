package tc.oc.pgm.filters.matcher.player;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Matches participants riding something: a boat, minecart, horse, another player...
 *
 * <pre>{@code <riding/>  <riding type="oak boat"/>}</pre>
 */
public class RidingFilter extends ParticipantFilter {

  public static final RidingFilter ANY = new RidingFilter(null);

  private final @Nullable EntityType type;

  public RidingFilter(@Nullable EntityType type) {
    this.type = type;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(VehicleEnterEvent.class, VehicleExitEvent.class);
  }

  @Override
  protected boolean matches(PlayerQuery query, MatchPlayer player) {
    Entity vehicle = player.getBukkit().getVehicle();
    return vehicle != null && (type == null || vehicle.getType() == type);
  }
}
