package tc.oc.pgm.platform.sportpaper.material;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.IBlockData;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.Painting;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Door;
import org.bukkit.util.BlockVector;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.platform.sportpaper.material.ModernMaterialNames.MaterialMapping;
import tc.oc.pgm.util.block.BlockData;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.ItemMaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.MaterialUtils;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

@Supports(SPORTPAPER)
@SuppressWarnings("deprecation")
public class SpMaterialUtils implements MaterialUtils {
  private static final int ENCODED_NULL_MATERIAL = -1;

  @Override
  public BlockMaterialData createBlockData(Material material) {
    if (!material.isBlock()) {
      throw new IllegalStateException("Material " + material + " is not a block");
    }
    return new SpMaterialData(material, (short) 0);
  }

  @Override
  public BlockMaterialData createBlockData(BlockState block) {
    return new SpMaterialData(block.getMaterialData());
  }

  @Override
  public BlockMaterialData createBlockData(ChunkSnapshot chunk, BlockVector chunkPos) {
    return new SpMaterialData(chunk.getMaterialData(chunkPos));
  }

  @Override
  public BlockMaterialData getTo(EntityChangeBlockEvent event) {
    return new SpMaterialData(event.getTo(), event.getData());
  }

  @Override
  public ItemMaterialData createItemData(ItemStack item) {
    return new SpMaterialData(item.getType(), item.getDurability());
  }

  @Override
  public ItemMaterialData createItemData(Material material, short data) {
    return new SpMaterialData(material, data);
  }

  @Override
  public ItemMaterialData createItemData(Hanging hanging) {
    return new SpMaterialData(
        switch (hanging) {
          case Painting ignored -> Material.PAINTING;
          case ItemFrame ignored -> Material.ITEM_FRAME;
          case LeashHitch ignored -> Material.LEASH;
          default -> null;
        });
  }

  @Override
  public BlockMaterialData decode(int encoded) {
    return SpEncodeUtil.decode(encoded);
  }

  @Override
  public Iterator<BlockData> iterator(
      Map<ChunkVector, ChunkSnapshot> chunks, Iterator<BlockVector> vectors) {
    return new BlockDataIterator(chunks, vectors);
  }

  @Override
  public Material parseMaterial(String text, @Nullable Node node) throws InvalidXMLException {
    return SpMaterialParser.parseMaterial(text, node);
  }

  @Override
  public ItemMaterialData parseItemMaterialData(String text, @Nullable Node node)
      throws InvalidXMLException {
    var md = SpMaterialParser.parseItem(text, node);
    validateItem(md.getItemType(), node);
    return md;
  }

  @Override
  public ItemMaterialData parseItemMaterialData(String text, short dmg, @Nullable Node node)
      throws InvalidXMLException {
    var md = SpMaterialParser.parseItem(text, node);
    short mdData = md.getData();
    if (mdData != dmg && mdData != 0 && dmg != 0)
      throw new InvalidXMLException(
          "Mismatching damage, parsed '" + text + ":" + dmg + "' but should be '" + text + ":"
              + md.getData() + "'",
          node);

    if (mdData != dmg && dmg != 0) md = new SpMaterialData(md.getItemType(), dmg);
    validateItem(md.getItemType(), node);
    return md;
  }

  public static boolean isItem(Material material) {
    return material == Material.AIR || CraftMagicNumbers.getItem(material) != null;
  }

  public static boolean isBlock(Material material) {
    return material.isBlock();
  }

  public static void validateItem(Material material, Node node) throws InvalidXMLException {
    if (!isItem(material)) {
      throw new InvalidXMLException("Material '" + material + "' is not an item", node);
    }
  }

  public static void validateBlock(Material material, Node node) throws InvalidXMLException {
    if (!isBlock(material)) {
      throw new InvalidXMLException("Material '" + material + "' is not a block", node);
    }
  }

  @Override
  public BlockMaterialData parseBlockMaterialData(String text, @Nullable Node node)
      throws InvalidXMLException {
    var md = SpMaterialParser.parseBlock(text, node);
    validateBlock(md.getItemType(), node);
    return md;
  }

  @Override
  public BlockMaterialData fromLegacyBlock(Material material, byte data) {
    return new SpMaterialData(material, data);
  }

  @Override
  public Set<BlockMaterialData> getPossibleBlocks(Material material) {
    // Get all possible blockstates off of nms
    Block block = CraftMagicNumbers.getBlock(material);
    List<IBlockData> states = block.P().a();
    Set<BlockMaterialData> materials = new HashSet<>(states.size());
    for (IBlockData state : states) {
      materials.add(new SpMaterialData(material, (byte) block.toLegacyData(state)));
    }
    return materials;
  }

  @Override
  public MaterialMatcher.Builder matcherBuilder() {
    return new MaterialMatcherBuilderImpl();
  }

  @Override
  public boolean isUpperHalfOfDoor(org.bukkit.block.Block block) {
    return block.getState().getData() instanceof Door door && door.isTopHalf();
  }

  private static class MaterialMatcherBuilderImpl extends MaterialMatcher.BuilderImpl
      implements SpMaterialParser.Adapter<MaterialMatcher.Builder> {

    @Override
    public MaterialMatcher.Builder visit(Material material) {
      if (blocksOnly && !isBlock(material))
        throw new InvalidMaterialException("Material '" + material + "' is not a block");
      return add(material);
    }

    @Override
    public MaterialMatcher.Builder visit(Material material, short data) {
      if (materialsOnly)
        throw new InvalidMaterialException(
            "Only supports materials, but got " + material + ":" + data);
      if (blocksOnly && !isBlock(material))
        throw new InvalidMaterialException("Material '" + material + "' is not a block");

      return add(new ExactMaterialMatcher(material, (byte) data));
    }

    @Override
    public MaterialMatcher.Builder visit(Node node, MaterialMapping mapping)
        throws InvalidXMLException {
      if (blocksOnly) return mapping.mapBlock(node, this::visit, this::visit);
      mapping.mapBoth(this::visit, this::visit);
      return this;
    }

    @Override
    public MaterialMatcher.Builder add(Material material, boolean flatten) {
      // No flattening required in legacy
      return add(material);
    }

    @Override
    public MaterialMatcher.Builder add(ItemStack item, boolean flatten) {
      return flatten
          ? visit(item.getType())
          : visit(item.getType(), item.getData().getData());
    }

    @Override
    protected void parseSingle(String text, @Nullable Node node) throws InvalidXMLException {
      try {
        SpMaterialParser.parse(text, node, materialsOnly, this);
      } catch (InvalidMaterialException e) {
        throw new InvalidXMLException(e.getMessage(), node);
      }
    }

    static class InvalidMaterialException extends RuntimeException {
      public InvalidMaterialException(String message) {
        super(message);
      }
    }
  }
}
