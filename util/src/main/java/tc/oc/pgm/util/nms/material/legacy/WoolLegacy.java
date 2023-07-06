package tc.oc.pgm.util.nms.material.legacy;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.nms.material.Wool;

public class WoolLegacy extends MaterialDataLegacy implements Wool {
  public WoolLegacy(MaterialData materialData) {
    super(materialData);
  }

  public WoolLegacy(Material material) {
    super(material);
  }

  @Override
  public @Nullable DyeColor getColor() {
    return DyeColor.getByWoolData(data);
  }

  @Override
  public void setColor(DyeColor color) {
    this.data = color.getWoolData();
  }
}
