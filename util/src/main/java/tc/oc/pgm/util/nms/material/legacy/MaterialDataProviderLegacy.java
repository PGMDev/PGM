package tc.oc.pgm.util.nms.material.legacy;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Minecart;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Banner;
import org.bukkit.material.Colorable;
import org.bukkit.material.Door;
import org.bukkit.material.PistonExtensionMaterial;
import org.bukkit.material.Rails;
import org.bukkit.material.Wool;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.MaterialDataProviderPlatform;

public class MaterialDataProviderLegacy implements MaterialDataProviderPlatform {
  @Override
  public MaterialData from(int hash) {
    return from(new org.bukkit.material.MaterialData(hash >> 8, (byte) hash));
  }

  @Override
  public MaterialData from(Block block) {
    return from(block.getState());
  }

  @Override
  public MaterialData from(BlockState blockState) {
    return from(blockState.getData());
  }

  @Override
  public MaterialData from(ItemStack itemStack) {
    return from(itemStack.getData());
  }

  @Override
  public MaterialData from(Material material) {
    switch (material) {
      case DARK_OAK_DOOR:
      case ACACIA_DOOR:
      case BIRCH_DOOR:
      case IRON_DOOR:
      case IRON_DOOR_BLOCK:
      case JUNGLE_DOOR:
      case SPRUCE_DOOR:
      case WOOD_DOOR:
      case WOODEN_DOOR:
        return new DoorLegacy(material);
      case WOOL:
        return new WoolLegacy(material);
      case BANNER:
      case STANDING_BANNER:
      case WALL_BANNER:
        return new BannerLegacy(material);
      case STAINED_CLAY:
      case STAINED_GLASS:
      case STAINED_GLASS_PANE:
      case CARPET:
        return new ColorableLegacy(material);
      case RAILS:
      case ACTIVATOR_RAIL:
      case DETECTOR_RAIL:
      case POWERED_RAIL:
        return new RailLegacy(material);
      case PISTON_BASE:
      case PISTON_EXTENSION:
      case PISTON_STICKY_BASE:
      case PISTON_MOVING_PIECE:
        return new PistonExtensionLegacy(material);
      case WATER:
      case STATIONARY_WATER:
      case LAVA:
      case STATIONARY_LAVA:
        return new LiquidLegacy(material);
      default:
        return new MaterialDataLegacy(material);
    }
  }

  @Override
  public MaterialData from(Material material, byte data) {
    return from(material.getNewData(data));
  }

  @Override
  public MaterialData from(Minecart minecart) {
    return from(minecart.getDisplayBlock());
  }

  @Override
  public MaterialData from(ChunkSnapshot chunkSnapshot, int x, int y, int z) {
    return from(
        new org.bukkit.material.MaterialData(
            chunkSnapshot.getBlockTypeId(x, y, z), (byte) chunkSnapshot.getBlockData(x, y, z)));
  }

  @NotNull
  private static MaterialDataLegacy from(org.bukkit.material.MaterialData data) {
    if (data instanceof Door) {
      return new DoorLegacy(data);
    } else if (data instanceof Wool) {
      return new WoolLegacy(data);
    } else if (data instanceof Banner) {
      return new BannerLegacy(data);
    } else if (data instanceof Colorable) {
      return new ColorableLegacy(data);
    } else if (data instanceof Rails) {
      return new RailLegacy(data);
    } else if (data instanceof PistonExtensionMaterial) {
      return new PistonExtensionLegacy(data);
    } else {
      switch (data.getItemType()) {
        case WATER:
        case STATIONARY_WATER:
        case LAVA:
        case STATIONARY_LAVA:
          return new LiquidLegacy(data.getItemType());
        default:
          return new MaterialDataLegacy(data);
      }
    }
  }

  @Override
  public MaterialData from(EntityChangeBlockEvent event) {
    return from(event.getTo().getNewData(event.getData()));
  }
}
