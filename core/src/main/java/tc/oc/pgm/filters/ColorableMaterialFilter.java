package tc.oc.pgm.filters;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import tc.oc.pgm.api.filter.query.MaterialQuery;

public class ColorableMaterialFilter extends TypedFilter<MaterialQuery> {
  @Override
  public Class<? extends MaterialQuery> getQueryType() {
    return MaterialQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(MaterialQuery query) {
    return QueryResponse.fromBoolean(matches(query.getMaterial()));
  }

  public boolean matches(Material type) {
    String typeString = type.name();
    int index = typeString.indexOf('_');
    if (index == -1) return false;

    String realType = typeString.substring(index);
    Material material = Material.getMaterial(DyeColor.WHITE.name() + realType);
    return material != null;
  }
}
