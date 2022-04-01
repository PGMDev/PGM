package tc.oc.pgm.filters;

import org.bukkit.Material;
import tc.oc.pgm.api.filter.query.MaterialQuery;

public class ColorableBlockFilter extends TypedFilter<MaterialQuery> {
  @Override
  public Class<? extends MaterialQuery> getQueryType() {
    return MaterialQuery.class
  }

  @Override
  protected QueryResponse queryTyped(MaterialQuery query) {
    return null;
  }

  public boolean matches(Material type) {
    return true;
  }
}
