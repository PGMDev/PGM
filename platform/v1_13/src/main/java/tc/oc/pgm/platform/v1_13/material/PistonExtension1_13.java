package tc.oc.pgm.platform.v1_13.material;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import tc.oc.pgm.util.nms.material.PistonExtension;

public class PistonExtension1_13 extends MaterialData1_13 implements PistonExtension {
  public PistonExtension1_13(Material material) {
    super(material);
  }

  public PistonExtension1_13(BlockData blockData) {
    super(blockData);
  }

  public PistonExtension1_13(Material material, boolean typeMatters) {
    super(material, typeMatters);
  }

  public PistonExtension1_13(Material material, boolean typeMatters, BlockData blockData) {
    super(material, typeMatters, blockData);
  }

  public PistonExtension1_13(
      Material material, BlockData blockData, Set<Material> similarMaterials) {
    super(material, blockData, similarMaterials);
  }

  @Override
  public void setFacingDirection(BlockFace direction) {
    this.blockData = getOrCreateBlockData();
    if (this.blockData instanceof Directional) {
      ((Directional) this.blockData).setFacing(direction);
    }
  }

  @Override
  public BlockFace getFacingDirection() {
    this.blockData = getOrCreateBlockData();
    return this.blockData instanceof Directional
        ? ((Directional) this.blockData).getFacing()
        : null;
  }

  @Override
  public MaterialData1_13 copy() {
    return new PistonExtension1_13(material, blockData, similarMaterials);
  }
}
