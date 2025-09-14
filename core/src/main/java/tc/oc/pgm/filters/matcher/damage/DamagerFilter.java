package tc.oc.pgm.filters.matcher.damage;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.DamageQuery;
import tc.oc.pgm.api.filter.query.EntityTypeQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.modifier.QueryModifier;
import tc.oc.pgm.tracker.info.EntityInfo;

public class DamagerFilter extends QueryModifier<DamageQuery, EntityTypeQuery> {

  public DamagerFilter(Filter child) {
    super(child, DamageQuery.class, EntityTypeQuery.class);
  }

  @Nullable
  @Override
  protected EntityTypeQuery transformQuery(DamageQuery query) {
    var damager = query.getDamageInfo().getDamager();
    if (damager instanceof EntityInfo) {
      return new EntityTypeQuery() {
        @Override
        public Class<? extends Entity> getEntityType() {
          return ((EntityInfo) damager).getEntityType().getEntityClass();
        }

        @Override
        public Event getEvent() {
          return query.getEvent();
        }

        @Override
        public Match getMatch() {
          return query.getMatch();
        }
      };
    }
    return null;
  }
}
