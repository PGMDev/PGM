package tc.oc.pgm.util.nms.material;

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

public interface MaterialData {
  Material getMaterial();

  boolean dataMatters();

  EntityChangeBlockEvent buildEntityChangeBlockEvent(Player player, Block block);

  int encode();

  void playStepEffect(Location location);

  boolean isBlock();

  void addToRecipe(ShapelessRecipe recipe, int count);

  void addToRecipe(ShapedRecipe recipe, char key);

  FurnaceRecipe constructFurnaceRecipe(ItemStack result);

  boolean matches(Block block);

  boolean matches(BlockState blockState);

  boolean matches(Material material);

  boolean matches(ItemStack itemStack);

  boolean matches(MaterialData materialData);

  ItemStack apply(ItemStack itemStack);

  BlockState apply(BlockState blockState);

  Block apply(Block block, boolean applyPhysics);

  void apply(Minecart minecart);

  ItemStack toItemStack(int count);

  FallingBlock spawnFallingBlock(Location location);
}
