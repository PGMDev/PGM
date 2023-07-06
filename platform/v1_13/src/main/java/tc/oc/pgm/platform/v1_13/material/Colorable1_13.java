package tc.oc.pgm.platform.v1_13.material;

import com.cryptomorin.xseries.XMaterial;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import tc.oc.pgm.util.Pair;
import tc.oc.pgm.util.nms.material.Colorable;

public class Colorable1_13 extends MaterialData1_13 implements Colorable {

  public Colorable1_13(Material material, boolean typeMatters) {
    super(material, typeMatters);
  }

  public Colorable1_13(BlockData blockData) {
    super(blockData);
  }

  public Colorable1_13(Material material, BlockData blockData, Set<Material> similarMaterials) {
    super(material, blockData, similarMaterials);
  }

  static Map<Material, DyeColor> materialDyeColorMap = new ConcurrentHashMap<>();
  static Map<String, Map<DyeColor, Material>> setColorMap = new ConcurrentHashMap<>();
  static Map<DyeColor, Material> inkSackMap = new ConcurrentHashMap<>();
  static Map<Pair<Material, DyeColor>, Material> setMaterialCache = new ConcurrentHashMap<>();

  static {
    for (Material material : Material.values()) {
      String materialName = material.name();
      if (materialName.startsWith("BLACK_")) {
        String strippedMaterial = materialName.substring(6);
        for (DyeColor dyeColor : DyeColor.values()) {
          Material matchMaterial = Material.matchMaterial(dyeColor.name() + "_" + strippedMaterial);
          if (matchMaterial != null) {
            materialDyeColorMap.put(matchMaterial, dyeColor);
          }

          setColorMap
              .computeIfAbsent(strippedMaterial, (mat) -> new HashMap<>())
              .put(dyeColor, matchMaterial);
        }
      }
    }
    for (byte i = 0; i < 16; i++) {
      XMaterial xMaterial = XMaterial.matchXMaterial("INK_SACK:" + i).get();
      materialDyeColorMap.put(xMaterial.parseMaterial(), DyeColor.getByDyeData(i));
      inkSackMap.put(DyeColor.getByDyeData(i), xMaterial.parseMaterial());
    }
  }

  @Override
  public DyeColor getColor() {
    return materialDyeColorMap.get(material);
  }

  @Override
  public void setColor(DyeColor color) {
    this.material =
        setMaterialCache.computeIfAbsent(
            new Pair<>(material, color),
            (materialDyeColorPair -> {
              if (inkSackMap.containsValue(material)) {
                return inkSackMap.get(color);
              }

              DyeColor existingDyeColor = materialDyeColorMap.get(material);
              String strippedMaterial = material.name().substring(existingDyeColor.name().length());
              return Material.matchMaterial(color.name() + strippedMaterial);
            }));
    BlockData oldBlockData = this.blockData;
    if (oldBlockData != null) {
      this.blockData = this.material.createBlockData();
      if (this.blockData instanceof Directional && oldBlockData instanceof Directional) {
        ((Directional) this.blockData).setFacing(((Directional) oldBlockData).getFacing());
      }
      if (this.blockData instanceof Rotatable && oldBlockData instanceof Rotatable) {
        ((Rotatable) this.blockData).setRotation(((Rotatable) oldBlockData).getRotation());
      }
    }
  }

  @Override
  public MaterialData1_13 copy() {
    return new Colorable1_13(this.material, this.blockData, this.similarMaterials);
  }
}
