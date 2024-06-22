package tc.oc.pgm.platform.modern;

import static tc.oc.pgm.util.nms.Packets.ENTITIES;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.destroystokyo.paper.profile.ProfileProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftFirework;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import tc.oc.pgm.platform.modern.attribute.AttributeMapBukkit;
import tc.oc.pgm.platform.modern.material.ModernBlockMaterialData;
import tc.oc.pgm.util.attribute.AttributeMap;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.skin.Skin;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernNMSHacks implements NMSHacks {
  @Override
  public void skipFireworksLaunch(Firework firework) {
    FireworkRocketEntity entityFirework = ((CraftFirework) firework).getHandle();
    entityFirework.lifetime = 2;
    entityFirework.life = 2;
    ENTITIES
        .entityMetadataPacket(firework.getEntityId(), firework, false)
        .sendToViewers(firework, false);
  }

  @Override
  public boolean isCraftItemArrowEntity(PlayerPickupItemEvent event) {
    return event instanceof PlayerPickupArrowEvent;
  }

  @Override
  public void freezeEntity(Entity entity) {
    if (((CraftEntity) entity).getHandle() instanceof Mob mob) {
      mob.setNoAi(true);
      mob.setNoGravity(true);
    }
  }

  @Override
  public void setFireballDirection(Fireball entity, Vector direction) {
    entity.setPower(direction.multiply(0.1D));
  }

  @Override
  public long getMonotonicTime(World world) {
    return ((CraftWorld) world).getHandle().getGameTime();
  }

  @Override
  public void resumeServer() {
    // no-op, server pausing is sportpaper-specific
  }

  @Override
  public Inventory createFakeInventory(Player viewer, Inventory realInventory) {
    Component customName;
    if (realInventory instanceof Nameable n && (customName = n.customName()) != null) {
      return realInventory instanceof DoubleChestInventory
          ? Bukkit.createInventory(viewer, realInventory.getSize(), customName)
          : Bukkit.createInventory(viewer, realInventory.getType(), customName);
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

    var nmsBlock = CraftMagicNumbers.getBlock(material);
    var chunk = craftChunk.getHandle(ChunkStatus.FULL);

    int baseY = chunk.getMinBuildHeight();
    for (int i = 0; i < chunk.getSections().length; i++) {
      var section = chunk.getSections()[i];
      if (section == null || section.hasOnlyAir()) continue;

      var states = section.getStates();
      if (!states.maybeHas(bs -> bs.getBukkitMaterial() == material)) continue;

      final int chunkY = baseY + (i * LevelChunkSection.SECTION_HEIGHT);

      // Iteration order is relevant, as indexes are packed as x | z << 4 | y << 8
      for (int y = 0; y < 16; y++) {
        for (int z = 0; z < 16; z++) {
          for (int x = 0; x < 16; x++) {
            if (states.get(x, y, z).getBlock() == nmsBlock)
              blocks.add(bukkitChunk.getBlock(x, chunkY + y, z));
          }
        }
      }
    }

    return blocks;
  }

  @Override
  public void setSkullMetaOwner(SkullMeta meta, String name, UUID uuid, Skin skin) {
    var profile = Bukkit.createProfile(uuid, name);
    profile.setProperty(new ProfileProperty("textures", skin.getData(), skin.getSignature()));
    meta.setPlayerProfile(profile);
  }

  @Override
  public WorldCreator detectWorld(String worldName) {
    // TODO: PLATFORM 1.20 read level nbt to get world gen settings
    return null;
  }

  @Override
  public boolean canMineBlock(BlockMaterialData blockMaterial, Player player) {
    // Alternative NMS method
    // var nmsPlayer = ((CraftPlayer) player).getHandle();
    // var nmsBlock = CraftMagicNumbers.getBlock(blockMaterial.getItemType());
    // return nmsPlayer.hasCorrectToolForDrops(nmsBlock.defaultBlockState());

    return ((ModernBlockMaterialData) blockMaterial)
        .getBlock()
        .isPreferredTool(player.getInventory().getItemInMainHand());
  }

  @Override
  public void resetDimension(World world) {
    // no-op
  }

  @Override
  public double getTPS() {
    return Bukkit.getServer().getTPS()[0];
  }

  @Override
  public AttributeMap buildAttributeMap(Player player) {
    return new AttributeMapBukkit(player);
  }

  @Override
  public void postToMainThread(Plugin plugin, boolean priority, Runnable task) {
    Bukkit.getServer().getScheduler().runTask(plugin, task);
  }

  @Override
  public int getMaxWorldSize(World world) {
    return ((CraftWorld) world).getHandle().getWorldBorder().getAbsoluteMaxSize();
  }

  @Override
  public int allocateEntityId() {
    return Bukkit.getUnsafe().nextEntityId();
  }
}
