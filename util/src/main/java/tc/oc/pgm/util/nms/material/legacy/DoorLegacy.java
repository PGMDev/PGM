package tc.oc.pgm.util.nms.material.legacy;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.util.nms.material.Door;

public class DoorLegacy extends MaterialDataLegacy implements Door {
  public DoorLegacy(MaterialData materialData) {
    super(materialData);
  }

  public DoorLegacy(Material material) {
    super(material);
  }

  @Override
  public boolean isTopHalf() {
    return (data & 0x8) == 0x8;
  }
}
