package tc.oc.pgm.util.material;

import java.util.Iterator;
import java.util.Map;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.block.BlockData;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.platform.Platform;

public interface MaterialData {

  Factory MATERIAL_DATA_FACTORY = Platform.requireInstance(Factory.class);

  static MaterialData from(Material material) {
    return MATERIAL_DATA_FACTORY.from(material);
  }

  static MaterialData from(Block block) {
    return MATERIAL_DATA_FACTORY.from(block.getState());
  }

  static MaterialData from(BlockState state) {
    return MATERIAL_DATA_FACTORY.from(state);
  }

  static MaterialData from(org.bukkit.material.MaterialData md) {
    return MATERIAL_DATA_FACTORY.from(md);
  }

  static MaterialData from(ChunkSnapshot chunk, BlockVector vector) {
    return MATERIAL_DATA_FACTORY.from(chunk, vector);
  }

  static MaterialData decode(int encoded) {
    return MATERIAL_DATA_FACTORY.decode(encoded);
  }

  static Iterator<BlockData> iterator(
      Map<ChunkVector, ChunkSnapshot> chunks, Iterator<BlockVector> vectors) {
    return MATERIAL_DATA_FACTORY.iterator(chunks, vectors);
  }

  Material getItemType();

  org.bukkit.material.MaterialData getBukkit();

  void applyTo(Block block, boolean update);

  void applyTo(BlockState block);

  void applyTo(ItemStack item);

  int encoded();

  interface Factory {
    MaterialData from(Material material);

    MaterialData from(BlockState block);

    MaterialData from(org.bukkit.material.MaterialData md);

    MaterialData from(ChunkSnapshot chunk, BlockVector vector);

    MaterialData decode(int encoded);

    MaterialData getTo(EntityChangeBlockEvent event);

    Iterator<BlockData> iterator(
        Map<ChunkVector, ChunkSnapshot> chunks, Iterator<BlockVector> vectors);
  }
}
