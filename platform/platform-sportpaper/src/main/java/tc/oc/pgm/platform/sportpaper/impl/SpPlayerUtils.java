package tc.oc.pgm.platform.sportpaper.impl;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.MovingObjectPosition;
import net.minecraft.server.v1_8_R3.Vec3D;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.platform.sportpaper.utils.Skins;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.util.nms.PlayerUtils;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.skin.Skin;

@Supports(SPORTPAPER)
public class SpPlayerUtils implements PlayerUtils {

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
}
