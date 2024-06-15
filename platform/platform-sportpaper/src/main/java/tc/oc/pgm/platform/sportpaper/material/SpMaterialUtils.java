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
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;
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
  public BlockMaterialData decode(int encoded) {
    if (encoded == ENCODED_NULL_MATERIAL) return null;
    Material material = Material.getMaterial(decodeTypeId(encoded));
    return new SpMaterialData(material, decodeMetadata(encoded));
  }

  static int decodeTypeId(int encoded) {
    return encoded & 0xfff;
  }

  static byte decodeMetadata(int encoded) {
    return (byte) (encoded >> 12);
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
    var md = SpMaterialParser.parsePgm(text, node);
    validateItem(md.getItemType(), node);
    return md;
  }

  @Override
  public ItemMaterialData parseItemMaterialData(String text, short dmg, @Nullable Node node)
      throws InvalidXMLException {
    var md = new SpMaterialData(SpMaterialParser.parseMaterial(text, node), dmg);
    validateItem(md.getItemType(), node);
    return md;
  }

  static void validateItem(Material material, Node node) throws InvalidXMLException {
    if (CraftMagicNumbers.getItem(material) == null) {
      throw new InvalidXMLException("Invalid item/block " + material, node);
    }
  }

  @Override
  public BlockMaterialData parseBlockMaterialData(String text, @Nullable Node node)
      throws InvalidXMLException {
    var md = SpMaterialParser.parsePgm(text, node);
    if (!md.getItemType().isBlock()) {
      throw new InvalidXMLException(
          "Material " + md.getItemType().name() + " is not a block", node);
    }
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
  public boolean hasBlockStates(Material material) {
    Block block = CraftMagicNumbers.getBlock(material);
    return !block.P().a().isEmpty();
  }

  @Override
  public MaterialMatcher.Builder matcherBuilder() {
    return new MaterialMatcherBuilderImpl();
  }

  @Override
  public void addIngredient(Node node, ShapelessRecipe recipe, int count)
      throws InvalidXMLException {
    recipe.addIngredient(count, SpMaterialParser.parseBukkit(node));
  }

  @Override
  public void setIngredient(Node node, ShapedRecipe recipe, char key) throws InvalidXMLException {
    recipe.setIngredient(key, SpMaterialParser.parseBukkit(node));
  }

  @Override
  public FurnaceRecipe createFurnaceRecipe(Node node, ItemStack result) throws InvalidXMLException {
    return new FurnaceRecipe(result, SpMaterialParser.parseBukkit(node));
  }

  private static class MaterialMatcherBuilderImpl extends MaterialMatcher.BuilderImpl
      implements SpMaterialParser.Adapter<MaterialMatcher.Builder> {

    @Override
    public MaterialMatcher.Builder visit(Material material) {
      return add(material);
    }

    @Override
    public MaterialMatcher.Builder visit(Material material, short data) {
      return add(new ExactMaterialMatcher(material, (byte) data));
    }

    @Override
    public MaterialMatcher.Builder add(Material material, boolean flatten) {
      // No flattening required in legacy
      return add(material);
    }

    @Override
    public MaterialMatcher.Builder add(ItemStack item, boolean flatten) {
      if (flatten) add(item.getType());
      else add(new ExactMaterialMatcher(item.getType(), item.getData().getData()));
      return this;
    }

    @Override
    protected void parseSingle(String text, @Nullable Node node) throws InvalidXMLException {
      SpMaterialParser.parse(text, node, materialsOnly, this);
    }
  }
}
