package tc.oc.pgm.filters;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import tc.oc.pgm.api.filter.query.EntityTypeQuery;

public class EntityTypeFilter extends TypedFilter<EntityTypeQuery> {
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
  public Class<? extends EntityTypeQuery> getQueryType() {
    return EntityTypeQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(EntityTypeQuery query) {
    return QueryResponse.fromBoolean(type.isAssignableFrom(query.getEntityType()));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{type=" + this.type.getSimpleName() + "}";
  }
}
