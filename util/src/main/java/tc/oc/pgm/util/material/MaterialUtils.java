package tc.oc.pgm.util.material;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.block.BlockData;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public interface MaterialUtils {
  MaterialUtils MATERIAL_UTILS = Platform.requireInstance(MaterialUtils.class);

  BlockMaterialData createBlockData(Material material);

  BlockMaterialData createBlockData(BlockState block);

  BlockMaterialData createBlockData(ChunkSnapshot chunk, BlockVector vector);

  BlockMaterialData getTo(EntityChangeBlockEvent event);

  ItemMaterialData createItemData(ItemStack item);

  ItemMaterialData createItemData(Material material, short data);

  BlockMaterialData decode(int encoded);

  Iterator<BlockData> iterator(
      Map<ChunkVector, ChunkSnapshot> chunks, Iterator<BlockVector> vectors);

  Material parseMaterial(String text, @Nullable Node node) throws InvalidXMLException;

  ItemMaterialData parseItemMaterialData(String text, @Nullable Node node)
      throws InvalidXMLException;

  ItemMaterialData parseItemMaterialData(String text, short dmg, @Nullable Node node)
      throws InvalidXMLException;

  BlockMaterialData parseBlockMaterialData(String text, @Nullable Node node)
      throws InvalidXMLException;

  BlockMaterialData fromLegacyBlock(Material material, byte data);

  Set<BlockMaterialData> getPossibleBlocks(Material material);

  boolean hasBlockStates(Material material);

  MaterialMatcher.Builder matcherBuilder();
}
