package tc.oc.pgm.filters.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.api.filter.query.MaterialQuery;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;

public class BlockFilter extends TypedFilter.Impl<MaterialQuery> {
  protected final Material type;
  protected final byte data; // values outside of the range 0 <= data < 16 indicate no preference

  public BlockFilter(SingleMaterialMatcher pattern) {
    this(pattern.getMaterial(), pattern.dataMatters() ? pattern.getData() : -1);
  }

  public BlockFilter(Material type) {
    this(type, (byte) -1);
  }

  public BlockFilter(Material type, byte data) {
    this.type = type;
    this.data = data;
  }

  @Override
  public Class<? extends MaterialQuery> queryType() {
    return MaterialQuery.class;
  }

  @Override
  public boolean matches(MaterialQuery query) {
    return matches(query.getMaterial());
  }

  public boolean matches(Material type, byte data) {
    if (this.type == type) {
      if (this.hasDataPreference()) {
        return this.data == data;
      } else {
        return true;
      }
    } else {
      return false;
    }
  }

  private boolean hasDataPreference() {
    return 0 <= this.data && this.data < 16;
  }

  @SuppressWarnings("deprecation")
  public boolean matches(MaterialData data) {
    return this.matches(data.getItemType(), data.getData());
  }

  public boolean matches(Material type) {
    return this.matches(type, (byte) 0);
  }

  @SuppressWarnings("deprecation")
  public boolean matches(BlockState state) {
    return this.matches(state.getType(), state.getRawData());
  }

  @SuppressWarnings("deprecation")
  public boolean matches(Block block) {
    return this.matches(block.getType(), block.getData());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("BlockFilter{");
    sb.append("type=").append(this.type);
    if (this.hasDataPreference()) {
      sb.append(",data=").append(this.data);
    }
    sb.append("}");
    return sb.toString();
  }
}
