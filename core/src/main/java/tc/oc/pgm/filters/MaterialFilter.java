package tc.oc.pgm.filters;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.api.filter.query.MaterialQuery;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;

public class MaterialFilter extends TypedFilter<MaterialQuery> {
  private final SingleMaterialMatcher pattern;

  public MaterialFilter(MaterialData materialData) {
    this(new SingleMaterialMatcher(materialData));
  }

  public MaterialFilter(Material material) {
    this(new SingleMaterialMatcher(material));
  }

  public MaterialFilter(SingleMaterialMatcher pattern) {
    this.pattern = pattern;
  }

  @Override
  public Class<? extends MaterialQuery> getQueryType() {
    return MaterialQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(MaterialQuery query) {
    return QueryResponse.fromBoolean(this.pattern.matches(query.getMaterial()));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName() + "{");
    sb.append("type=").append(this.pattern.getMaterial());
    if (this.pattern.dataMatters()) {
      sb.append(",data=").append(this.pattern.getData());
    }
    sb.append("}");
    return sb.toString();
  }
}
