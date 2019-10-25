package tc.oc.pgm.modules;

import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.Filter.QueryResponse;
import tc.oc.pgm.filters.query.EntitySpawnQuery;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;

public class MobsMatchModule extends MatchModule implements Listener {
  private final Filter mobsFilter;

  public MobsMatchModule(Match match, Filter mobsFilter) {
    super(match);
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
