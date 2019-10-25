package tc.oc.pgm.filters;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import tc.oc.pgm.filters.query.IEntityTypeQuery;

public class EntityTypeFilter extends TypedFilter<IEntityTypeQuery> {
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
  public Class<? extends IEntityTypeQuery> getQueryType() {
    return IEntityTypeQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(IEntityTypeQuery query) {
    return QueryResponse.fromBoolean(type.isAssignableFrom(query.getEntityType()));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{type=" + this.type.getSimpleName() + "}";
  }
}
