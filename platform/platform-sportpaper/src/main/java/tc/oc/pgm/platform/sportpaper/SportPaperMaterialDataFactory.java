package tc.oc.pgm.platform.sportpaper;

import java.util.Iterator;
import java.util.Map;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.block.BlockData;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.platform.Supports;

@Supports(Supports.Variant.SPORTPAPER)
@SuppressWarnings("deprecation")
public class SportPaperMaterialDataFactory implements MaterialData.Factory {
  int ENCODED_NULL_MATERIAL = -1;

  @Override
  public MaterialData from(Material material) {
    return new SportPaperMaterialData(new org.bukkit.material.MaterialData(material));
  }

  @Override
  public MaterialData from(BlockState block) {
    return new SportPaperMaterialData(block.getMaterialData());
  }

  @Override
  public MaterialData from(org.bukkit.material.MaterialData md) {
    return new SportPaperMaterialData(md);
  }

  @Override
  public MaterialData from(ChunkSnapshot chunk, BlockVector chunkPos) {
    return new SportPaperMaterialData(
        new org.bukkit.material.MaterialData(
            chunk.getBlockTypeId(chunkPos.getBlockX(), chunkPos.getBlockY(), chunkPos.getBlockZ()),
            (byte)
                chunk.getBlockData(
                    chunkPos.getBlockX(), chunkPos.getBlockY(), chunkPos.getBlockZ())));
  }

  @Override
  public MaterialData decode(int encoded) {
    if (encoded == ENCODED_NULL_MATERIAL) return null;
    Material material = Material.getMaterial(decodeTypeId(encoded));
    if (material.getData() == org.bukkit.material.MaterialData.class) {
      return new SportPaperMaterialData(
          new org.bukkit.material.MaterialData(material, decodeMetadata(encoded)));
    } else {
      return new SportPaperMaterialData(material.getNewData(decodeMetadata(encoded)));
    }
  }

  static int decodeTypeId(int encoded) {
    return encoded & 0xfff;
  }

  static byte decodeMetadata(int encoded) {
    return (byte) (encoded >> 12);
  }

  @Override
  public MaterialData getTo(EntityChangeBlockEvent event) {
    return new SportPaperMaterialData(
        new org.bukkit.material.MaterialData(event.getTo(), event.getData()));
  }

  @Override
  public Iterator<BlockData> iterator(
      Map<ChunkVector, ChunkSnapshot> chunks, Iterator<BlockVector> vectors) {
    return new BlockDataIterator(chunks, vectors);
  }
}
