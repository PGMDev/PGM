package tc.oc.pgm.modules;

import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.Filter.QueryResponse;
import tc.oc.pgm.filters.query.EntitySpawnQuery;

@ListenerScope(MatchScope.LOADED)
public class MobsMatchModule implements MatchModule, Listener {
  private final Match match;
  private final Filter mobsFilter;

  public MobsMatchModule(Match match, Filter mobsFilter) {
    this.match = match;
    this.mobsFilter = mobsFilter;
  }

  @Override
  public void enable() {
    this.match.getWorld().setSpawnFlags(true, true);
  }

  @Override
  public void disable() {
    this.match.getWorld().setSpawnFlags(false, false);
  }

  @EventHandler(ignoreCancelled = true)
  public void checkSpawn(final CreatureSpawnEvent event) {
    if (!(event.getEntity() instanceof ArmorStand)) {
      QueryResponse response =
          this.mobsFilter.query(
              new EntitySpawnQuery(event, event.getEntity(), event.getSpawnReason()));
      event.setCancelled(response.isDenied());
    }
  }
}
