package tc.oc.pgm.filters.matcher.entity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import tc.oc.pgm.api.filter.query.EntityTypeQuery;
import tc.oc.pgm.filters.matcher.TypedFilter;

public class EntityTypeFilter extends TypedFilter.Impl<EntityTypeQuery> {
  private final Class<? extends Entity> type;

  public EntityTypeFilter(Class<? extends Entity> type) {
    this.type = type;
  }

  public EntityTypeFilter(EntityType type) {
    this(type.getEntityClass());
  }

  public Class<? extends Entity> getEntityType() {
    return type;
  }

  @Override
  public Class<? extends EntityTypeQuery> queryType() {
    return EntityTypeQuery.class;
  }

  @Override
  public boolean matches(EntityTypeQuery query) {
    return type.isAssignableFrom(query.getEntityType());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{type=" + this.type.getSimpleName() + "}";
  }
}
