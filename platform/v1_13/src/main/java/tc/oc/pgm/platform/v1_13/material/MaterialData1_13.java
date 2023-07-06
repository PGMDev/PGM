package tc.oc.pgm.platform.v1_13.material;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import tc.oc.pgm.util.nms.material.MaterialData;

public class MaterialData1_13 implements MaterialData {

  BlockData blockData;
  Material material;
  Set<Material> similarMaterials;

  public MaterialData1_13(Material material) {
    this(material, true);
  }

  public MaterialData1_13(BlockData blockData) {
    this(blockData.getMaterial(), true, blockData);
  }

  public MaterialData1_13(Material material, boolean typeMatters) {
    this(material, typeMatters, null);
  }

  public MaterialData1_13(Material material, boolean typeMatters, BlockData blockData) {
    this.material = material;
    this.blockData = blockData;
    if (typeMatters || !LegacyMaterialUtils.DATA_RELEVANT_MATERIALS.contains(material)) {
      this.similarMaterials = EnumSet.of(this.material);
    } else {
      this.similarMaterials = LegacyMaterialUtils.getSimilarMaterials(material);
    }
  }

  public MaterialData1_13(Material material, BlockData blockData, Set<Material> similarMaterials) {
    this.material = material;
    this.blockData = blockData;
    this.similarMaterials = similarMaterials;
  }

  @Override
  public Material getMaterial() {
    return this.material;
  }

  @Override
  public boolean dataMatters() {
    return this.similarMaterials.size() > 1;
  }

  @Override
  public EntityChangeBlockEvent buildEntityChangeBlockEvent(Player player, Block block) {
    return new EntityChangeBlockEvent(player, block, getOrCreateBlockData());
  }

  @Override
  public int encode() {
    return material.ordinal();
  }

  @Override
  public void playStepEffect(Location location) {
    location.getWorld().playEffect(location, Effect.STEP_SOUND, material);
  }

  @Override
  public boolean isBlock() {
    return material.isBlock();
  }

  @Override
  public void addToRecipe(ShapelessRecipe recipe, int count) {
    recipe.addIngredient(count, material);
  }

  @Override
  public void addToRecipe(ShapedRecipe recipe, char key) {
    recipe.setIngredient(key, material);
  }

  @Override
  public FurnaceRecipe constructFurnaceRecipe(ItemStack result) {
    // TODO: used NameSpacedKey
    return new FurnaceRecipe(result, material);
  }

  @Override
  public boolean matches(Block block) {
    return this.similarMaterials.contains(block.getType());
  }

  @Override
  public boolean matches(BlockState blockState) {
    return this.similarMaterials.contains(blockState.getType());
  }

  @Override
  public boolean matches(Material material) {
    return this.similarMaterials.contains(material);
  }

  @Override
  public boolean matches(ItemStack itemStack) {
    return this.similarMaterials.contains(itemStack.getType());
  }

  @Override
  public boolean matches(MaterialData materialData) {
    return this.similarMaterials.contains(materialData.getMaterial());
  }

  @Override
  public ItemStack apply(ItemStack itemStack) {
    itemStack.setType(material);
    return itemStack;
  }

  @Override
  public BlockState apply(BlockState blockState) {
    blockState.setType(material);
    if (blockData != null) {
      blockState.setBlockData(blockData);
    }
    return blockState;
  }

  @Override
  public Block apply(Block block, boolean applyPhysics) {
    block.setType(material, applyPhysics);
    if (blockData != null) {
      block.setBlockData(blockData);
    }
    return block;
  }

  @Override
  public void apply(Minecart minecart) {
    minecart.setDisplayBlockData(getOrCreateBlockData());
  }

  @Override
  public ItemStack toItemStack(int count) {
    return new ItemStack(material, count);
  }

  @Override
  public FallingBlock spawnFallingBlock(Location location) {
    location.getWorld().spawnFallingBlock(location, getOrCreateBlockData());
    return null;
  }

  protected BlockData getOrCreateBlockData() {
    if (blockData == null) {
      return material.createBlockData();
    }
    return blockData;
  }

  public MaterialData1_13 copy() {
    return new MaterialData1_13(this.material, this.blockData, this.similarMaterials);
  }

  @Override
  public String toString() {
    return "MaterialData1_13{"
        + "blockData="
        + blockData
        + ", material="
        + material
        + ", similarMaterials="
        + similarMaterials
        + '}';
  }
}
