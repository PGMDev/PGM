package tc.oc.pgm.platform.sportpaper;

import static tc.oc.pgm.util.nms.Packets.ENTITIES;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.EntityArrow;
import net.minecraft.server.v1_8_R3.EntityFireball;
import net.minecraft.server.v1_8_R3.EntityFireworks;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.IDataManager;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.ServerNBTManager;
import net.minecraft.server.v1_8_R3.WorldData;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFireball;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFirework;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftItem;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import tc.oc.pgm.platform.sportpaper.attribute.SpAttributeMap;
import tc.oc.pgm.util.attribute.AttributeMap;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.reflect.ReflectionUtils;
import tc.oc.pgm.util.skin.Skin;

@Supports(SPORTPAPER)
public class NMSHacksSportPaper implements NMSHacks {
  @Override
  public void skipFireworksLaunch(Firework firework) {
    EntityFireworks entityFirework = ((CraftFirework) firework).getHandle();
    entityFirework.expectedLifespan = 2;
    entityFirework.ticksFlown = 2;
    ENTITIES
        .entityMetadataPacket(firework.getEntityId(), firework, false)
        .sendToViewers(firework, false);
  }

  @Override
  public boolean isCraftItemArrowEntity(Item item) {
    return ((CraftItem) item).getHandle() instanceof EntityArrow;
  }

  @Override
  public void freezeEntity(Entity entity) {
    net.minecraft.server.v1_8_R3.Entity nmsEntity = ((CraftEntity) entity).getHandle();
    NBTTagCompound tag = new NBTTagCompound();
    nmsEntity.c(tag); // save to tag
    tag.setBoolean("NoAI", true);
    tag.setBoolean("NoGravity", true);
    nmsEntity.f(tag); // load from tag
  }

  @Override
  public void setFireballDirection(Fireball entity, Vector direction) {
    EntityFireball fireball = ((CraftFireball) entity).getHandle();
    fireball.dirX = direction.getX() * 0.1D;
    fireball.dirY = direction.getY() * 0.1D;
    fireball.dirZ = direction.getZ() * 0.1D;
  }

  @Override
  public long getMonotonicTime(World world) {
    return ((CraftWorld) world).getHandle().getTime();
  }

  @Override
  public void updateChunkSnapshot(ChunkSnapshot snapshot, BlockState blockState) {
    snapshot.updateBlock(blockState);
  }

  @Override
  public void resumeServer() {
    if (Bukkit.getServer().isSuspended()) Bukkit.getServer().setSuspended(false);
  }

  @Override
  public Inventory createFakeInventory(Player viewer, Inventory realInventory) {
    if (realInventory.hasCustomName()) {
      return realInventory instanceof DoubleChestInventory
          ? Bukkit.createInventory(viewer, realInventory.getSize(), realInventory.getName())
          : Bukkit.createInventory(viewer, realInventory.getType(), realInventory.getName());
    } else {
      return realInventory instanceof DoubleChestInventory
          ? Bukkit.createInventory(viewer, realInventory.getSize())
          : Bukkit.createInventory(viewer, realInventory.getType());
    }
  }

  @Override
  public Set<Block> getBlocks(Chunk bukkitChunk, Material material) {
    CraftChunk craftChunk = (CraftChunk) bukkitChunk;
    Set<Block> blocks = new HashSet<>();

    net.minecraft.server.v1_8_R3.Block nmsBlock = CraftMagicNumbers.getBlock(material);
    net.minecraft.server.v1_8_R3.Chunk chunk = craftChunk.getHandle();

    for (ChunkSection section : chunk.getSections()) {
      if (section == null || section.a()) continue; // ChunkSection.a() -> true if section is empty

      char[] blockIds = section.getIdArray();
      for (int i = 0; i < blockIds.length; i++) {
        // This does a lookup in the block registry, but does not create any objects, so should be
        // pretty efficient
        IBlockData blockData = net.minecraft.server.v1_8_R3.Block.d.a(blockIds[i]);
        if (blockData != null && blockData.getBlock() == nmsBlock) {
          blocks.add(
              bukkitChunk.getBlock(i & 0xf, section.getYPosition() | (i >> 8), (i >> 4) & 0xf));
        }
      }
    }

    return blocks;
  }

  @Override
  public void setSkullMetaOwner(SkullMeta meta, String name, UUID uuid, Skin skin) {
    meta.setOwner(name, uuid, new org.bukkit.Skin(skin.getData(), skin.getSignature()));
  }

  @Override
  public WorldCreator detectWorld(String worldName) {
    IDataManager sdm =
        new ServerNBTManager(Bukkit.getServer().getWorldContainer(), worldName, true);
    WorldData worldData = sdm.getWorldData();
    if (worldData == null) return null;

    return new WorldCreator(worldName)
        .generateStructures(worldData.shouldGenerateMapFeatures())
        .generatorSettings(worldData.getGeneratorOptions())
        .seed(worldData.getSeed())
        .type(org.bukkit.WorldType.getByName(worldData.getType().name()));
  }

  @Override
  public boolean canMineBlock(MaterialData blockMaterial, ItemStack tool) {
    if (!blockMaterial.getItemType().isBlock()) {
      throw new IllegalArgumentException("Material '" + blockMaterial + "' is not a block");
    }

    net.minecraft.server.v1_8_R3.Block nmsBlock =
        CraftMagicNumbers.getBlock(blockMaterial.getItemType());
    net.minecraft.server.v1_8_R3.Item nmsTool =
        tool == null ? null : CraftMagicNumbers.getItem(tool.getType());

    return nmsBlock != null
        && (nmsBlock.getMaterial().isAlwaysDestroyable()
            || (nmsTool != null && nmsTool.canDestroySpecialBlock(nmsBlock)));
  }

  @Override
  public void resetDimension(World world) {
    try {
      ((CraftWorld) world).getHandle().dimension = 11;
    } catch (IllegalAccessError e) {

      Field worldServerField = ReflectionUtils.getField(CraftWorld.class, "world");
      Field dimensionField = ReflectionUtils.getField(WorldServer.class, "dimension");
      Field modifiersField = ReflectionUtils.getField(Field.class, "modifiers");

      try {
        modifiersField.setInt(dimensionField, dimensionField.getModifiers() & ~Modifier.FINAL);
        dimensionField.set(worldServerField.get(world), 11);
      } catch (IllegalAccessException ex) {
        // No-op, newer version of Java have disabled modifying final fields
      }
    }
  }

  @Override
  public double getTPS() {
    return Bukkit.getServer().spigot().getTPS()[0];
  }

  @Override
  public AttributeMap buildAttributeMap(Player player) {
    return new SpAttributeMap(player);
  }

  @Override
  public void postToMainThread(Plugin plugin, boolean priority, Runnable task) {
    Bukkit.getServer().postToMainThread(plugin, true, task);
  }

  @Override
  public int getMaxWorldSize(World world) {
    return ((CraftWorld) world).getHandle().getWorldBorder().l();
  }

  @Override
  public void addRecipe(World world, Recipe recipe) {
    world.addRecipe(recipe);
  }

  @Override
  public void resetRecipes(World world) {
    world.resetRecipes();
  }
}
