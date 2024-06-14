package tc.oc.pgm.util.material;

import static tc.oc.pgm.util.material.MaterialUtils.MATERIAL_UTILS;

import java.util.Iterator;
import java.util.Map;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.block.BlockData;
import tc.oc.pgm.util.chunk.ChunkVector;

public interface MaterialData {
  BlockMaterialData AIR = MaterialData.block(Material.AIR);

  static MaterialData of(Material material) {
    return MATERIAL_UTILS.createBlockData(material);
  }

  static BlockMaterialData block(Material material) {
    return MATERIAL_UTILS.createBlockData(material);
  }

  static BlockMaterialData block(Block block) {
    return MATERIAL_UTILS.createBlockData(block.getState());
  }

  static BlockMaterialData block(BlockState state) {
    return MATERIAL_UTILS.createBlockData(state);
  }

  static BlockMaterialData block(ChunkSnapshot chunk, BlockVector vector) {
    return MATERIAL_UTILS.createBlockData(chunk, vector);
  }

  static ItemMaterialData item(ItemStack item) {
    return MATERIAL_UTILS.createItemData(item);
  }

  static ItemMaterialData item(Material material, short data) {
    return MATERIAL_UTILS.createItemData(material, data);
  }

  static BlockMaterialData decode(int encoded) {
    return MATERIAL_UTILS.decode(encoded);
  }

  static Iterator<BlockData> iterator(
      Map<ChunkVector, ChunkSnapshot> chunks, Iterator<BlockVector> vectors) {
    return MATERIAL_UTILS.iterator(chunks, vectors);
  }

  Material getItemType();

  MaterialMatcher toMatcher();
}
