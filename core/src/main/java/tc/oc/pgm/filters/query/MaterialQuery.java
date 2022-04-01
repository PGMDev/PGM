package tc.oc.pgm.filters.query;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.event.Event;

public class MaterialQuery extends Query implements tc.oc.pgm.api.filter.query.MaterialQuery {

  private final Material material;

  public MaterialQuery(@Nullable Event event, Material material) {
    super(event);
    this.material = checkNotNull(material);
  }

  @Override
  public Material getMaterial() {
    return material;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MaterialQuery)) return false;
    MaterialQuery query = (MaterialQuery) o;
    if (!material.equals(query.material)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return material.hashCode();
  }

  private static final Map<Material, MaterialQuery> cache = new HashMap<>();

  public static MaterialQuery get(Material material) {
    MaterialQuery query = cache.get(material);
    if (query == null) {
      query = new MaterialQuery(null, material);
      cache.put(material, query);
    }
    return query;
  }
}
