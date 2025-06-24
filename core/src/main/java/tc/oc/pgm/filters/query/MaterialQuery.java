package tc.oc.pgm.filters.query;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.material.MaterialData;

public class MaterialQuery extends Query implements tc.oc.pgm.api.filter.query.MaterialQuery {

  private final MaterialData material;

  // Use MaterialQuery#get for a cached instance
  private MaterialQuery(@Nullable Event event, MaterialData material) {
    super(event);
    this.material = assertNotNull(material);
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
