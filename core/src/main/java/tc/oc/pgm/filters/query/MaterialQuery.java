package tc.oc.pgm.filters.query;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.event.Event;
import org.bukkit.material.MaterialData;

public class MaterialQuery extends Query implements IMaterialQuery {

  private final MaterialData material;

  public MaterialQuery(@Nullable Event event, MaterialData material) {
    super(event);
    this.material = checkNotNull(material);
  }

  @Override
  public MaterialData getMaterial() {
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

  private static final Map<MaterialData, MaterialQuery> cache = new HashMap<>();

  public static MaterialQuery get(MaterialData material) {
    MaterialQuery query = cache.get(material);
    if (query == null) {
      query = new MaterialQuery(null, material);
      cache.put(material, query);
    }
    return query;
  }
}
