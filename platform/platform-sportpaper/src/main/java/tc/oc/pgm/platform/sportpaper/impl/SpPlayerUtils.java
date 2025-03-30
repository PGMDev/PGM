package tc.oc.pgm.platform.sportpaper.impl;

import static org.bukkit.craftbukkit.v1_8_R3.util.LongHash.lsw;
import static org.bukkit.craftbukkit.v1_8_R3.util.LongHash.msw;
import static org.bukkit.craftbukkit.v1_8_R3.util.LongHash.toLong;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;
import static tc.oc.pgm.util.reflect.ReflectionUtils.getField;
import static tc.oc.pgm.util.reflect.ReflectionUtils.setField;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.MovingObjectPosition;
import net.minecraft.server.v1_8_R3.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_8_R3.Vec3D;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.platform.sportpaper.material.LegacyMaterialData;
import tc.oc.pgm.platform.sportpaper.packets.PacketSender;
import tc.oc.pgm.platform.sportpaper.utils.Skins;
import tc.oc.pgm.util.block.BlockVectorSet;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.nms.PlayerUtils;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.skin.Skin;

@Supports(SPORTPAPER)
public class SpPlayerUtils implements PlayerUtils, PacketSender {

  @Override
  public boolean teleportRelative(
      Player player,
      Vector deltaPos,
      float deltaYaw,
      float deltaPitch,
      PlayerTeleportEvent.TeleportCause cause) {
    return player.teleportRelative(deltaPos, deltaYaw, deltaPitch, cause);
  }

  @Override
  public Skin getPlayerSkin(Player player) {
    CraftPlayer craftPlayer = (CraftPlayer) player;
    return Skins.fromProfile(craftPlayer.getProfile());
  }

  @Override
  public Skin getPlayerSkinForViewer(Player player, Player viewer) {
    return player.hasFakeSkin(viewer)
        ? new Skin(
            player.getFakeSkin(viewer).getData(), player.getFakeSkin(viewer).getSignature())
        : getPlayerSkin(player);
  }

  @Override
  public String getPlayerName(UUID uuid) {
    GameProfile profile = MinecraftServer.getServer().getUserCache().a(uuid);
    return profile == null ? null : profile.getName();
  }

  @Override
  public void setAbsorption(LivingEntity entity, double health) {
    ((CraftLivingEntity) entity).getHandle().setAbsorptionHearts((float) health);
  }

  @Override
  public double getAbsorption(LivingEntity entity) {
    return ((CraftLivingEntity) entity).getHandle().getAbsorptionHearts();
  }

  @Override
  public void showInvisibles(Player player, boolean showInvisibles) {
    player.showInvisibles(showInvisibles);
  }

  @Override
  public void setAffectsSpawning(Player player, boolean affectsSpawning) {
    player.spigot().setAffectsSpawning(affectsSpawning);
  }

  @Override
  public void setCollidesWithEntities(Player player, boolean collides) {
    player.spigot().setCollidesWithEntities(collides);
  }

  @Override
  public void setKnockbackReduction(Player player, float reduction) {
    player.setKnockbackReduction(reduction);
  }

  @Override
  public float getKnockbackReduction(Player player) {
    return player.getKnockbackReduction();
  }

  @Override
  public void clearArrowsInPlayer(Player player) {
    player.setArrowsStuck(0);
  }

  @Override
  public int getPing(Player player) {
    return ((CraftPlayer) player).getHandle().ping;
  }

  @Override
  public void setPotionParticles(Player player, boolean enabled) {
    player.setPotionParticles(enabled);
  }

  @Override
  public RayBlockIntersection getTargetedBlock(Player player) {
    Location start = player.getEyeLocation();
    World world = player.getWorld();
    Vector startVector = start.toVector();
    Vector end = start
        .toVector()
        .add(start.getDirection().multiply(player.getGameMode() == GameMode.CREATIVE ? 6 : 4.5));
    MovingObjectPosition hit = ((CraftWorld) world)
        .getHandle()
        .rayTrace(
            new Vec3D(startVector.getX(), startVector.getY(), startVector.getZ()),
            new Vec3D(end.getX(), end.getY(), end.getZ()),
            false,
            false,
            false);
    if (hit != null && hit.type == MovingObjectPosition.EnumMovingObjectType.BLOCK) {
      return new RayBlockIntersection(
          world.getBlockAt(hit.a().getX(), hit.a().getY(), hit.a().getZ()),
          CraftBlock.notchToBlockFace(hit.direction),
          new Vector(hit.pos.a, hit.pos.b, hit.pos.c));
    } else {
      return null;
    }
  }

  @Override
  public void sendMultiBlockPacket(
      Player pl, BlockVectorSet positions, @Nullable BlockMaterialData data) {
    // Build a map of chunk -> block[]
    Map<Long, ShortArrayList> sectionMap = new HashMap<>();

    var locCache = new Location(null, 0, 0, 0);
    positions.getLongSet().forEach((long encoded) -> {
      BlockVectors.decodeInto(encoded, locCache);
      int x = locCache.getBlockX(), y = locCache.getBlockY(), z = locCache.getBlockZ();
      sectionMap
          .computeIfAbsent(toLong(x >> 4, z >> 4), k -> new ShortArrayList())
          .add((short) ((x & 15) << 12 | (z & 15) << 8 | y));
    });

    // Send each chunk
    if (data instanceof LegacyMaterialData l) {
      var blockData = CraftMagicNumbers.getBlock(data.getItemType()).fromLegacyData(l.getData());
      for (Map.Entry<Long, ShortArrayList> entry : sectionMap.entrySet())
        send(multiBlock(entry.getKey(), entry.getValue(), blockData), pl);
    } else {
      for (Map.Entry<Long, ShortArrayList> entry : sectionMap.entrySet()) {
        var chunk = (CraftChunk) pl.getWorld().getChunkAt(msw(entry.getKey()), lsw(entry.getKey()));
        var bl = entry.getValue();
        send(new PacketPlayOutMultiBlockChange(bl.size(), bl.elements(), chunk.getHandle()), pl);
      }
    }
  }

  private static final Field CHUNK = getField(PacketPlayOutMultiBlockChange.class, "a");
  private static final Field BLOCKS = getField(PacketPlayOutMultiBlockChange.class, "b");

  private PacketPlayOutMultiBlockChange multiBlock(long ch, ShortList blocks, IBlockData data) {
    var packet = new PacketPlayOutMultiBlockChange();
    var changes = new PacketPlayOutMultiBlockChange.MultiBlockChangeInfo[blocks.size()];
    for (int i = 0; i < blocks.size(); i++)
      changes[i] = packet.new MultiBlockChangeInfo(blocks.getShort(i), data);

    setField(packet, new ChunkCoordIntPair(msw(ch), lsw(ch)), CHUNK);
    setField(packet, changes, BLOCKS);
    return packet;
  }
}
