package tc.oc.pgm.util.nms.material.legacy;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.util.nms.material.Rail;

public class RailLegacy extends MaterialDataLegacy implements Rail {
  public RailLegacy(MaterialData materialData) {
    super(materialData);
  }

  public RailLegacy(Material material) {
    super(material);
  }

  @Override
  public boolean dataIsValid() {
    return !(data < 0 || data >= 10);
  }

  @Override
  public byte getDirectionIndex() {
    return data;
  }
}
