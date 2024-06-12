package tc.oc.pgm.util.nms;

import java.util.Set;
import java.util.UUID;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.attribute.AttributeMap;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.skin.Skin;

public interface NMSHacks {
  NMSHacks NMS_HACKS = Platform.requireInstance(NMSHacks.class);

  void skipFireworksLaunch(Firework firework);

  boolean isCraftItemArrowEntity(Item item);

  void freezeEntity(Entity entity);

  void setFireballDirection(Fireball entity, Vector direction);

  void updateChunkSnapshot(ChunkSnapshot snapshot, BlockState blockState);

  long getMonotonicTime(World world);

  void resumeServer();

  Inventory createFakeInventory(Player viewer, Inventory realInventory);

  Set<Block> getBlocks(Chunk bukkitChunk, Material material);

  void setSkullMetaOwner(SkullMeta meta, String name, UUID uuid, Skin skin);

  WorldCreator detectWorld(String worldName);

  boolean canMineBlock(MaterialData blockMaterial, ItemStack tool);

  void resetDimension(World world);

  double getTPS();

  AttributeMap buildAttributeMap(Player player);

  void postToMainThread(Plugin plugin, boolean priority, Runnable task);

  int getMaxWorldSize(World world);

  void addRecipe(World world, Recipe recipe);

  void resetRecipes(World world);
}
