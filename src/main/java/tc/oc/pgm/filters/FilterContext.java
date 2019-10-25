package tc.oc.pgm.filters;

import org.bukkit.entity.LivingEntity;
import tc.oc.pgm.filters.query.IBlockQuery;
import tc.oc.pgm.filters.query.IEntitySpawnQuery;
import tc.oc.pgm.filters.query.IEntityTypeQuery;
import tc.oc.pgm.filters.query.IPlayerQuery;
import tc.oc.util.collection.ContextStore;

public class FilterContext extends ContextStore<Filter> {
  public FilterContext() {
    this.add("allow-all", StaticFilter.ALLOW);
    this.add("deny-all", StaticFilter.DENY);
    this.addDefaultFilter("players", new QueryTypeFilter(IPlayerQuery.class));
    this.addDefaultFilter("blocks", new QueryTypeFilter(IBlockQuery.class));
    this.addDefaultFilter("world", new LegacyWorldFilter());
    this.addDefaultFilter("spawns", new QueryTypeFilter(IEntitySpawnQuery.class));
    this.addDefaultFilter("entities", new QueryTypeFilter(IEntityTypeQuery.class));
    this.addDefaultFilter("mobs", new EntityTypeFilter(LivingEntity.class));
  }

  private void addDefaultFilter(String name, Filter filter) {
    this.add("allow-" + name, new AllowFilter(filter));
    this.add("deny-" + name, new DenyFilter(filter));
  }
}
