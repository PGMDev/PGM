package tc.oc.pgm.filters;

import org.bukkit.entity.LivingEntity;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.BlockQuery;
import tc.oc.pgm.api.filter.query.EntitySpawnQuery;
import tc.oc.pgm.api.filter.query.EntityTypeQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.util.collection.ContextStoreImpl;

public class FilterContext extends ContextStoreImpl<Filter> {
  public FilterContext() {
    this.add("allow-all", StaticFilter.ALLOW);
    this.add("deny-all", StaticFilter.DENY);
    this.addDefaultFilter("players", new QueryTypeFilter(PlayerQuery.class));
    this.addDefaultFilter("blocks", new QueryTypeFilter(BlockQuery.class));
    this.addDefaultFilter("world", new LegacyWorldFilter());
    this.addDefaultFilter("spawns", new QueryTypeFilter(EntitySpawnQuery.class));
    this.addDefaultFilter("entities", new QueryTypeFilter(EntityTypeQuery.class));
    this.addDefaultFilter("mobs", new EntityTypeFilter(LivingEntity.class));
  }

  private void addDefaultFilter(String name, Filter filter) {
    this.add("allow-" + name, new AllowFilter(filter));
    this.add("deny-" + name, new DenyFilter(filter));
  }
}
