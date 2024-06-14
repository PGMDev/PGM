package tc.oc.pgm.platform.v1_20_6.material;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.ItemMaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.MaterialUtils;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

@Supports(value = PAPER, minVersion = "1.20.6")
@SuppressWarnings("deprecation")
public class ModernMaterialUtils implements MaterialUtils {
  private static final int ENCODED_NULL_MATERIAL = -1;

  @Override
  public BlockMaterialData createBlockData(Material material) {
    if (!material.isBlock()) {
      throw new IllegalStateException("Material " + material + " is not a block");
    }
    return new ModernBlockData(material.createBlockData());
  }

  @Override
  public BlockMaterialData createBlockData(BlockState block) {
    return new ModernBlockData(block.getBlockData());
  }

  @Override
  public BlockMaterialData createBlockData(ChunkSnapshot chunk, BlockVector chunkPos) {
    return new ModernBlockData(
        chunk.getBlockData(chunkPos.getBlockX(), chunkPos.getBlockY(), chunkPos.getBlockZ()));
  }

  @Override
  public BlockMaterialData getTo(EntityChangeBlockEvent event) {
    return new ModernBlockData(event.getBlockData());
  }

  @Override
  public ItemMaterialData createItemData(ItemStack item) {
    return new ModernItemData(item.getType());
  }

  @Override
  public ItemMaterialData createItemData(Material material, short data) {
    return ModernMaterialParser.parseItem(material, data);
  }

  @Override
  public BlockMaterialData decode(int encoded) {
    if (encoded == ENCODED_NULL_MATERIAL) return null;
    return new ModernBlockData(Material.values()[encoded].createBlockData());
  }

  @Override
  public Iterator<tc.oc.pgm.util.block.BlockData> iterator(
      Map<ChunkVector, ChunkSnapshot> chunks, Iterator<BlockVector> vectors) {
    return new BlockDataIterator(chunks, vectors);
  }

  @Override
  public Material parseMaterial(String text, @Nullable Node node) throws InvalidXMLException {
    return Bukkit.getUnsafe().fromLegacy(ModernMaterialParser.parseMaterial(text, node));
  }

  @Override
  public ItemMaterialData parseItemMaterialData(String text, @Nullable Node node)
      throws InvalidXMLException {
    var md = ModernMaterialParser.parseItem(text, node);
    if (CraftMagicNumbers.getItem(md.getItemType()) == null) {
      throw new InvalidXMLException("Invalid item/block", node);
    }
    return md;
  }

  @Override
  public ItemMaterialData parseItemMaterialData(String text, short dmg, @Nullable Node node)
      throws InvalidXMLException {
    var md = ModernMaterialParser.parseItem(text, dmg, node);
    if (CraftMagicNumbers.getItem(md.getItemType()) == null) {
      throw new InvalidXMLException("Invalid item/block", node);
    }
    return md;
  }

  @Override
  public BlockMaterialData parseBlockMaterialData(String text, @Nullable Node node)
      throws InvalidXMLException {
    var md = ModernMaterialParser.parseBlock(text, node);
    if (!md.getItemType().isBlock()) {
      throw new InvalidXMLException(
          "Material " + md.getItemType().name() + " is not a block", node);
    }
    return md;
  }

  @Override
  public BlockMaterialData fromLegacyBlock(Material material, byte data) {
    return new ModernBlockData(Bukkit.getUnsafe().fromLegacy(material, data));
  }

  @Override
  public Set<BlockMaterialData> getPossibleBlocks(Material material) {
    // Get all possible blockstates off of nms
    var block = CraftMagicNumbers.getBlock(material);
    var states = block.getStateDefinition().getPossibleStates();
    Set<BlockMaterialData> materials = new HashSet<>(states.size());
    for (var state : states) {
      materials.add(new ModernBlockData(state.createCraftBlockData()));
    }
    return materials;
  }

  @Override
  public boolean hasBlockStates(Material material) {
    var block = CraftMagicNumbers.getBlock(material);
    return !block.getStateDefinition().getPossibleStates().isEmpty();
  }

  @Override
  public MaterialMatcher.Builder matcherBuilder() {
    return new MaterialMatcherBuilderImpl();
  }

  @Override
  public void addIngredient(Node node, ShapelessRecipe recipe, int count)
      throws InvalidXMLException {
    var materials = ModernMaterialParser.parseFlatten(node);
    while (count-- > 0) {
      recipe.addIngredient(new RecipeChoice.MaterialChoice(materials));
    }
  }

  @Override
  public void setIngredient(Node node, ShapedRecipe recipe, char key) throws InvalidXMLException {
    var materials = ModernMaterialParser.parseFlatten(node);
    recipe.setIngredient(key, new RecipeChoice.MaterialChoice(materials));
  }

  @Override
  public FurnaceRecipe createFurnaceRecipe(Node node, ItemStack result) throws InvalidXMLException {
    var materials = ModernMaterialParser.parseFlatten(node);
    return new FurnaceRecipe(
        NamespacedKey.randomKey(), result, new RecipeChoice.MaterialChoice(materials), 0, 200);
  }

  private static class MaterialMatcherBuilderImpl extends MaterialMatcher.BuilderImpl
      implements ModernMaterialParser.Adapter<MaterialMatcher.Builder> {

    @Override
    public MaterialMatcher.Builder visit(Material material) {
      return addAll(ModernMaterialParser.flatten(material));
    }

    @Override
    public MaterialMatcher.Builder visit(Material material, short data) {
      BlockData bd = Bukkit.getUnsafe().fromLegacy(material, (byte) data);
      if (MATERIAL_UTILS.hasBlockStates(bd.getMaterial())) {
        return add(new BlockStateMaterialMatcher(bd));
      } else {
        return add(bd.getMaterial());
      }
    }

    @Override
    public MaterialMatcher.Builder add(Material material, boolean flatten) {
      // TODO: PLATFORM 1.20 - flatten non-legacy itemstack into list of modern
      return add(material);
    }

    @Override
    public MaterialMatcher.Builder add(ItemStack item, boolean flatten) {
      // TODO: PLATFORM 1.20 - flatten non-legacy itemstack into list of modern
      return add(item.getType());
    }

    @Override
    protected void parseSingle(String text, @Nullable Node node) throws InvalidXMLException {
      ModernMaterialParser.parse(text, node, materialsOnly, this);
    }
  }
}
