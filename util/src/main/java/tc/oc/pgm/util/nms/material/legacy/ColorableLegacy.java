package tc.oc.pgm.util.nms.material.legacy;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.nms.material.Colorable;

public class ColorableLegacy extends MaterialDataLegacy implements Colorable {
  public ColorableLegacy(MaterialData materialData) {
    super(materialData);
  }

  public ColorableLegacy(Material material) {
    super(material);
  }

  @Override
  public @Nullable DyeColor getColor() {
    return DyeColor.getByWoolData(data);
  }

  @Override
  public void setColor(DyeColor color) {
    if (Material.INK_SACK.equals(material)) {
      this.data = (byte) (15 - color.getWoolData());
    } else {
      this.data = color.getWoolData();
    }
  }
}
