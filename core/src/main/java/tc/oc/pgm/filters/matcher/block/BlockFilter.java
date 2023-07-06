package tc.oc.pgm.filters.matcher.block;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.filter.query.MaterialQuery;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.MaterialDataProvider;

public class BlockFilter extends TypedFilter.Impl<MaterialQuery> {
  protected final MaterialData materialData;

  public BlockFilter(SingleMaterialMatcher pattern) {
    this.materialData = pattern.getMaterialData();
  }

  public BlockFilter(Material type) {
    this.materialData = MaterialDataProvider.from(type);
  }

  @Override
  public Class<? extends MaterialQuery> queryType() {
    return MaterialQuery.class;
  }

  @Override
  public boolean matches(MaterialQuery query) {
    return matches(query.getMaterial());
  }

  public boolean matches(ItemStack itemStack) {
    return this.matches(MaterialDataProvider.from(itemStack));
  }

  public boolean matches(MaterialData data) {
    return this.materialData.matches(data);
  }

  @Override
  public String toString() {
    return "BlockFilter{material=" + this.materialData + "}";
  }
}
