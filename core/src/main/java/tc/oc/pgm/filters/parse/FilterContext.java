package tc.oc.pgm.filters.parse;

import org.bukkit.entity.LivingEntity;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.BlockQuery;
import tc.oc.pgm.api.filter.query.EntitySpawnQuery;
import tc.oc.pgm.api.filter.query.EntityTypeQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.filters.matcher.QueryTypeFilter;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.matcher.entity.EntityTypeFilter;
import tc.oc.pgm.filters.matcher.entity.LegacyWorldFilter;
import tc.oc.pgm.filters.operator.AllowFilter;
import tc.oc.pgm.filters.operator.DenyFilter;
import tc.oc.pgm.util.collection.ContextStore;

public class FilterContext extends ContextStore<Filter> {
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
