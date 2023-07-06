package tc.oc.pgm.util.nms.material.legacy;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.MaterialDataProvider;

public class MaterialDataLegacy implements MaterialData, MaterialDataProvider {

  protected Material material;
  protected byte data;
  protected boolean dataMatters;

  public MaterialDataLegacy(org.bukkit.material.MaterialData materialData) {
    this.material = materialData.getItemType();
    this.data = materialData.getData();
    this.dataMatters = true;
  }

  public MaterialDataLegacy(Material material, byte data) {
    this.material = material;
    this.data = data;
    this.dataMatters = true;
  }

  public MaterialDataLegacy(Material material) {
    this.material = material;
    this.data = 0;
    this.dataMatters = false;
  }

  @Override
  public Material getMaterial() {
    return material;
  }

  @Override
  public boolean dataMatters() {
    return dataMatters;
  }

  @Override
  public EntityChangeBlockEvent buildEntityChangeBlockEvent(Player player, Block block) {
    return new EntityChangeBlockEvent(player, block, material, data);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MaterialDataLegacy) {
      MaterialDataLegacy other = (MaterialDataLegacy) obj;
      if (material.equals(other.material)) {
        if (dataMatters) {
          return data == other.data;
        } else {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return encode();
  }

  @Override
  public int encode() {
    return ((material.getId() << 8) & data);
  }

  public static int encodedMaterialAt(ChunkSnapshot chunkSnapshot, int x, int y, int z) {
    int blockTypeId = chunkSnapshot.getBlockTypeId(x, y, z);
    int data = chunkSnapshot.getBlockData(x, y, z);
    return (blockTypeId << 8) & data;
  }

  @Override
  public void playStepEffect(Location location) {
    location.getWorld().playEffect(location, Effect.STEP_SOUND, material.getId() + (data << 12));
  }

  @Override
  public boolean isBlock() {
    return material.isBlock();
  }

  @Override
  public void addToRecipe(ShapelessRecipe recipe, int count) {
    if (dataMatters) {
      recipe.addIngredient(count, this.material.getNewData(data));
    } else {
      recipe.addIngredient(count, this.material);
    }
  }

  @Override
  public void addToRecipe(ShapedRecipe recipe, char key) {
    if (dataMatters) {
      recipe.setIngredient(key, this.material.getNewData(data));
    } else {
      recipe.setIngredient(key, this.material);
    }
  }

  @Override
  public FurnaceRecipe constructFurnaceRecipe(ItemStack result) {
    if (dataMatters) {
      return new FurnaceRecipe(result, this.material.getNewData(data));
    } else {
      return new FurnaceRecipe(result, this.material);
    }
  }

  @Override
  public boolean matches(Block block) {
    return this.material.equals(block.getType())
        && (!this.dataMatters || this.data == block.getData());
  }

  @Override
  public boolean matches(BlockState blockState) {
    return this.material.equals(blockState.getType())
        && (!this.dataMatters || this.data == blockState.getRawData());
  }

  @Override
  public boolean matches(Material material) {
    return this.material.equals(material);
  }

  @Override
  public boolean matches(ItemStack itemStack) {
    return this.material.equals(itemStack.getType())
        && (!this.dataMatters || this.data == itemStack.getData().getData());
  }

  @Override
  public boolean matches(MaterialData materialData) {
    return this.equals(materialData);
  }

  @Override
  public ItemStack apply(ItemStack itemStack) {
    itemStack.setType(material);
    itemStack.setData(material.getNewData(data));
    return itemStack; // for chaining
  }

  @Override
  public BlockState apply(BlockState blockState) {
    if (BukkitUtils.isSportPaper()) {
      blockState.setMaterialData(this.material.getNewData(data));
    } else {
      blockState.setType(material);
      blockState.setRawData(data);
    }
    return blockState; // for chaining
  }

  @Override
  public Block apply(Block block, boolean applyPhysics) {
    block.setTypeIdAndData(material.getId(), data, applyPhysics);
    return block; // for chaining
  }

  @Override
  public void apply(Minecart minecart) {
    minecart.setDisplayBlock(material.getNewData(data));
  }

  @Override
  public ItemStack toItemStack(int count) {
    return new ItemStack(material, count, (short) 0, data);
  }

  @Override
  public FallingBlock spawnFallingBlock(Location location) {
    return location.getWorld().spawnFallingBlock(location, material, data);
  }
}
