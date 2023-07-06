package tc.oc.pgm.platform.v1_13.material;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.collect.Maps;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Piston;
import org.bukkit.entity.Minecart;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.MaterialDataProviderPlatform;

public class MaterialDataProvider1_13 implements MaterialDataProviderPlatform {

  static Map<String, MaterialData1_13> materialDataCache = Maps.newConcurrentMap();

  static {
    // XMaterial gets a few incorrect due to cross version material name collision
    materialDataCache.put(
        "STONE_SLAB2", new MaterialData1_13(XMaterial.RED_SANDSTONE_SLAB.parseMaterial(), true));
  }

  @Override
  public MaterialData1_13 from(int hash) {
    return from(Material.values()[hash]);
  }

  @Override
  public MaterialData1_13 from(Block block) {
    return from(block.getState());
  }

  @Override
  public MaterialData1_13 from(BlockState blockState) {
    return from(blockState.getBlockData());
  }

  public MaterialData1_13 from(BlockData blockData) {
    if (blockData instanceof Door) {
      return new Door1_13(blockData);
    } else if (blockData instanceof Piston) {
      return new PistonExtension1_13(blockData);
    } else if (blockData instanceof Rail) {
      return new Rail1_13(blockData);
    } else if (blockData.getMaterial().name().endsWith("_BANNER")) {
      return new Banner1_13(blockData);
    } else if (LegacyMaterialUtils.getSimilarMaterials(Material.WHITE_WOOL)
        .contains(blockData.getMaterial())) {
      return new Wool1_13(blockData);
    } else if (Colorable1_13.materialDyeColorMap.containsKey(blockData.getMaterial())) {
      return new Colorable1_13(blockData);
    }

    return new MaterialData1_13(blockData);
  }

  private MaterialData1_13 from(Material material, boolean typeMatters) {
    if (material == null) {
      return null;
    }
    if (LegacyMaterialUtils.getSimilarMaterials(Material.WHITE_WOOL).contains(material)) {
      return new Wool1_13(material, typeMatters);
    } else if (Colorable1_13.materialDyeColorMap.containsKey(material)) {
      return new Colorable1_13(material, typeMatters);
    } else if (material.name().endsWith("_BANNER")) {
      return new Banner1_13(material);
    } else if (material.isBlock()) {
      BlockData blockData = material.createBlockData();
      if (blockData instanceof Door) {
        return new Door1_13(material, typeMatters);
      } else if (blockData instanceof Piston) {
        return new PistonExtension1_13(blockData);
      } else if (blockData instanceof Rail) {
        return new Rail1_13(blockData);
      }
    }

    return new MaterialData1_13(material, typeMatters);
  }

  @Override
  public MaterialData1_13 from(ItemStack itemStack) {
    return from(itemStack.getType());
  }

  @Override
  public MaterialData1_13 from(Material material) {
    return from(material, true);
  }

  @Override
  public MaterialData1_13 from(Material material, byte data) {
    throw new UnsupportedOperationException("You cannot call from with data for 1.13");
  }

  @Override
  public MaterialData1_13 from(Minecart minecart) {
    return from(minecart.getDisplayBlockData());
  }

  @Override
  public MaterialData1_13 from(ChunkSnapshot chunkSnapshot, int x, int y, int z) {
    return from(chunkSnapshot.getBlockData(x, y, z));
  }

  @Override
  public MaterialData1_13 from(EntityChangeBlockEvent event) {
    return from(event.getBlockData());
  }

  @Override
  public MaterialData from(String parsingMaterial) {
    parsingMaterial = parsingMaterial.toUpperCase(Locale.ROOT).replace(" ", "_");
    MaterialData1_13 materialResult =
        materialDataCache.computeIfAbsent(
            parsingMaterial,
            (materialString) -> {
              Material material = Material.matchMaterial(materialString);
              if (material != null) return from(material);
              boolean dataMatters = false;

              String[] parts = materialString.split(":");
              String processedMaterialString = parts[0];
              if (processedMaterialString.matches("-?\\d+")) {
                processedMaterialString =
                    LegacyMaterialUtils.idToMaterialString(
                        Integer.parseInt(processedMaterialString));
              }
              Optional<XMaterial> xMaterialOption;
              if (parts.length > 1) {
                dataMatters = true;
                xMaterialOption =
                    XMaterial.matchXMaterial(processedMaterialString + ":" + parts[1]);
                if (!xMaterialOption.isPresent() || !xMaterialOption.get().isSupported()) {
                  xMaterialOption = XMaterial.matchXMaterial(processedMaterialString + ":0");
                }
                if (!xMaterialOption.isPresent() || !xMaterialOption.get().isSupported()) {
                  xMaterialOption = XMaterial.matchXMaterial(processedMaterialString);
                }
              } else {
                xMaterialOption = XMaterial.matchXMaterial(processedMaterialString);
                if (!xMaterialOption.isPresent() || !xMaterialOption.get().isSupported()) {
                  xMaterialOption = XMaterial.matchXMaterial(processedMaterialString + ":0");
                }
              }

              if (xMaterialOption.isPresent()) {
                XMaterial xMaterial = xMaterialOption.get();
                material = xMaterial.parseMaterial();

                if (!dataMatters) {
                  Optional<XMaterial> testMatters =
                      XMaterial.matchXMaterial(
                          processedMaterialString.substring(0, processedMaterialString.length())
                              + ":1");
                  if (testMatters.isPresent()) {
                    if (!material.equals(testMatters.get().parseMaterial())) {
                      dataMatters = true;
                    }
                  }
                }

                return from(material, dataMatters);
              } else {
                return null;
              }
            });
    if (materialResult == null) {
      return null;
    }
    return materialResult.copy();
  }
}
