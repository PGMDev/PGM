package tc.oc.pgm.util.nms.material.legacy;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.util.nms.material.Banner;

public class BannerLegacy extends MaterialDataLegacy implements Banner {

  public BannerLegacy(MaterialData materialData) {
    super(materialData);
  }

  public BannerLegacy(Material material) {
    super(material);
  }

  @Override
  public void setFacingDirection(BlockFace direction) {
    if (isWallBanner()) {
      switch (direction) {
        case NORTH:
          data = 0x2;
          break;

        case SOUTH:
          data = 0x3;
          break;

        case WEST:
          data = 0x4;
          break;

        case EAST:
        default:
          data = 0x5;
      }
    } else {
      switch (direction) {
        case SOUTH:
          data = 0x0;
          break;

        case SOUTH_SOUTH_WEST:
          data = 0x1;
          break;

        case SOUTH_WEST:
          data = 0x2;
          break;

        case WEST_SOUTH_WEST:
          data = 0x3;
          break;

        case WEST:
          data = 0x4;
          break;

        case WEST_NORTH_WEST:
          data = 0x5;
          break;

        case NORTH_WEST:
          data = 0x6;
          break;

        case NORTH_NORTH_WEST:
          data = 0x7;
          break;

        case NORTH:
          data = 0x8;
          break;

        case NORTH_NORTH_EAST:
          data = 0x9;
          break;

        case NORTH_EAST:
          data = 0xA;
          break;

        case EAST_NORTH_EAST:
          data = 0xB;
          break;

        case EAST:
          data = 0xC;
          break;

        case EAST_SOUTH_EAST:
          data = 0xD;
          break;

        case SOUTH_SOUTH_EAST:
          data = 0xF;
          break;

        case SOUTH_EAST:
        default:
          data = 0xE;
      }
    }
  }

  @Override
  public BlockFace getFacingDirection() {
    if (!isWallBanner()) {
      switch (data) {
        case 0x0:
          return BlockFace.SOUTH;

        case 0x1:
          return BlockFace.SOUTH_SOUTH_WEST;

        case 0x2:
          return BlockFace.SOUTH_WEST;

        case 0x3:
          return BlockFace.WEST_SOUTH_WEST;

        case 0x4:
          return BlockFace.WEST;

        case 0x5:
          return BlockFace.WEST_NORTH_WEST;

        case 0x6:
          return BlockFace.NORTH_WEST;

        case 0x7:
          return BlockFace.NORTH_NORTH_WEST;

        case 0x8:
          return BlockFace.NORTH;

        case 0x9:
          return BlockFace.NORTH_NORTH_EAST;

        case 0xA:
          return BlockFace.NORTH_EAST;

        case 0xB:
          return BlockFace.EAST_NORTH_EAST;

        case 0xC:
          return BlockFace.EAST;

        case 0xD:
          return BlockFace.EAST_SOUTH_EAST;

        case 0xE:
          return BlockFace.SOUTH_EAST;

        case 0xF:
          return BlockFace.SOUTH_SOUTH_EAST;
      }

      return null;
    } else {
      return getAttachedFace().getOppositeFace();
    }
  }

  private boolean isWallBanner() {
    return material == Material.WALL_BANNER;
  }

  private BlockFace getAttachedFace() {
    if (isWallBanner()) {
      switch (data) {
        case 0x2:
          return BlockFace.SOUTH;

        case 0x3:
          return BlockFace.NORTH;

        case 0x4:
          return BlockFace.EAST;

        case 0x5:
          return BlockFace.WEST;
      }

      return null;
    } else {
      return BlockFace.DOWN;
    }
  }
}
