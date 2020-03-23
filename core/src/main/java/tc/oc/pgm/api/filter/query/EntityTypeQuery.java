package tc.oc.pgm.api.filter.query;

import org.bukkit.entity.Entity;

public interface EntityTypeQuery extends MatchQuery {
  Class<? extends Entity> getEntityType();
}
