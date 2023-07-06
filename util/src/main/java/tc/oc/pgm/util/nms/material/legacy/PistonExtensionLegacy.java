package tc.oc.pgm.util.nms.material.legacy;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.util.nms.material.PistonExtension;

public class PistonExtensionLegacy extends MaterialDataLegacy implements PistonExtension {
  public PistonExtensionLegacy(MaterialData materialData) {
    super(materialData);
  }

  public PistonExtensionLegacy(Material material) {
    super(material);
  }

  @Override
  public BlockFace getFacingDirection() {
    byte dir = (byte) (data & 7);

    switch (dir) {
      case 0:
        return BlockFace.DOWN;
      case 1:
        return BlockFace.UP;
      case 2:
        return BlockFace.NORTH;
      case 3:
        return BlockFace.SOUTH;
      case 4:
        return BlockFace.WEST;
      case 5:
        return BlockFace.EAST;
      default:
        return BlockFace.SELF;
    }
  }

  @Override
  public void setFacingDirection(BlockFace direction) {
    if (material == Material.PISTON_EXTENSION) {
      data = (byte) (data & 0x8);

      switch (direction) {
        case UP:
          data |= 1;
          break;
        case NORTH:
          data |= 2;
          break;
        case SOUTH:
          data |= 3;
          break;
        case WEST:
          data |= 4;
          break;
        case EAST:
          data |= 5;
          break;
      }
    } else {
      throw new UnsupportedOperationException(
          "setFacingDirection not yet implemented for Material: " + this.material);
    }
  }
}
