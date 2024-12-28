package tc.oc.pgm.filters.matcher.block;

import org.bukkit.Material;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.MaterialQuery;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.filters.operator.InverseFilter;
import tc.oc.pgm.util.material.MaterialMatcher;

public class MaterialFilter extends TypedFilter.Impl<MaterialQuery> {
  public static final Filter NOT_AIR =
      new InverseFilter(new MaterialFilter(MaterialMatcher.of(Material.AIR)));

  private final MaterialMatcher pattern;

  public MaterialFilter(MaterialMatcher pattern) {
    this.pattern = pattern;
  }

  public MaterialMatcher getPattern() {
    return pattern;
  }

  @Override
  public Class<? extends MaterialQuery> queryType() {
    return MaterialQuery.class;
  }

  @Override
  public boolean matches(MaterialQuery query) {
    return this.pattern.matches(query.getMaterial());
  }

  @Override
  public String toString() {
    return "MaterialFilter{pattern=" + this.pattern + "}";
  }
}
