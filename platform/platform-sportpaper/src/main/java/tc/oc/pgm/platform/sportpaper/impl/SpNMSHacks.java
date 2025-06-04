package tc.oc.pgm.platform.sportpaper.impl;

import static tc.oc.pgm.util.nms.Packets.ENTITIES;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.*;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFireball;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFirework;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftItem;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.chunk.NullChunkGenerator;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.reflect.ReflectionUtils;
import tc.oc.pgm.util.skin.Skin;

@Supports(SPORTPAPER)
public class SpNMSHacks implements NMSHacks {
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
  public boolean isCraftItemArrowEntity(PlayerPickupItemEvent event) {
    return ((CraftItem) event.getItem()).getHandle() instanceof EntityArrow;
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
  public List<Block> getBlocks(Chunk bukkitChunk, Material material) {
    CraftChunk craftChunk = (CraftChunk) bukkitChunk;
    List<Block> blocks = new ArrayList<>();

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
  public World createWorld(String worldName, World.Environment env, boolean terrain, long seed) {
    WorldCreator creator = new WorldCreator(worldName);

    IDataManager sdm =
        new ServerNBTManager(Bukkit.getServer().getWorldContainer(), worldName, true);
    WorldData worldData = sdm.getWorldData();
    if (worldData != null) {
      creator
          .generateStructures(worldData.shouldGenerateMapFeatures())
          .generatorSettings(worldData.getGeneratorOptions())
          .seed(worldData.getSeed())
          .type(org.bukkit.WorldType.getByName(worldData.getType().name()));
    }

    return Bukkit.getServer()
        .createWorld(creator
            .environment(env)
            .generator(terrain ? null : NullChunkGenerator.INSTANCE)
            .seed(terrain ? seed : creator.seed()));
  }

  @Override
  public boolean canMineBlock(BlockMaterialData blockMaterial, Player player) {
    ItemStack tool = player.getItemInHand();

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
    var nmsWorld = ((CraftWorld) world).getHandle();
    try {
      nmsWorld.dimension = 11;
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
  public void cleanupWorld(World world) {
    var nmsWorld = ((CraftWorld) world).getHandle();
    nmsWorld.craftingManager.lastCraftView = null;
    world.setKeepSpawnInMemory(false);
  }

  @Override
  public void cleanupPlayer(Player player) {
    var nmsPlayer = ((CraftPlayer) player).getHandle();
    nmsPlayer.killer = null;
    nmsPlayer.p(null); // Resets who last hit the entityLiving
  }

  @Override
  public double getTPS() {
    return Bukkit.getServer().spigot().getTPS()[0];
  }

  @Override
  public void postToMainThread(Plugin plugin, boolean priority, Runnable task) {
    Bukkit.getServer().postToMainThread(plugin, priority, task);
  }

  @Override
  public int getMaxWorldSize(World world) {
    return ((CraftWorld) world).getHandle().getWorldBorder().l();
  }

  @Override
  public int allocateEntityId() {
    return Bukkit.allocateEntityId();
  }

  @Override
  public Object getBlockNBT(Block block) {
    TileEntity tile = ((CraftWorld) block.getWorld())
        .getHandle()
        .getTileEntity(new BlockPosition(block.getX(), block.getY(), block.getZ()));
    if (tile == null) return null;

    NBTTagCompound tag = new NBTTagCompound();
    tile.b(tag); // Save
    return tag;
  }

  @Override
  public void setBlockNBT(Block block, Object tag) {
    if (!(tag instanceof NBTTagCompound)) return;

    WorldServer world = ((CraftWorld) block.getWorld()).getHandle();
    BlockPosition pos = new BlockPosition(block.getX(), block.getY(), block.getZ());
    NBTTagCompound compound = (NBTTagCompound) tag;

    compound.setInt("x", pos.getX());
    compound.setInt("y", pos.getY());
    compound.setInt("z", pos.getZ());

    TileEntity tile = world.getTileEntity(pos);
    if (tile == null) {
      // Force block update to trigger tile entity re-creation
      IBlockData blockData = world.getType(pos);
      world.setTypeAndData(pos, Blocks.AIR.getBlockData(), 0); // Clear block
      world.setTypeAndData(pos, blockData, 3); // Restore block

      tile = world.getTileEntity(pos);
      if (tile == null) {
        return;
      }
    }

    tile.a(compound); // Load NBT
    tile.update();
  }
}
