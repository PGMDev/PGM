package tc.oc.pgm.filters;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import tc.oc.pgm.api.filter.query.MaterialQuery;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;

public class BlockFilter extends TypedFilter<MaterialQuery> {
  protected final Material type;

  public BlockFilter(SingleMaterialMatcher pattern) {
    this(pattern.getMaterial());
  }

  public BlockFilter(Material type) {
    this.type = type;
  }

  @Override
  public Class<? extends MaterialQuery> getQueryType() {
    return MaterialQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(MaterialQuery query) {
    return QueryResponse.fromBoolean(matches(query.getMaterial()));
  }

  public boolean matches(Material type) {
    return this.type == type;
  }

  @SuppressWarnings("deprecation")
  public boolean matches(BlockState state) {
    return this.matches(state.getType());
  }

  @SuppressWarnings("deprecation")
  public boolean matches(Block block) {
    return this.matches(block.getType());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("BlockFilter{");
    sb.append("type=").append(this.type);
    sb.append("}");
    return sb.toString();
  }
}
