package tc.oc.pgm.filters;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.filters.query.IMaterialQuery;
import tc.oc.util.bukkit.material.matcher.SingleMaterialMatcher;

public class MaterialFilter extends TypedFilter<IMaterialQuery> {
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
  public Class<? extends IMaterialQuery> getQueryType() {
    return IMaterialQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(IMaterialQuery query) {
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
