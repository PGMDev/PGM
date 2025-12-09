package tc.oc.pgm.util.nms;

import java.util.UUID;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.block.BlockVectorSet;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.skin.Skin;

public interface PlayerUtils {
  PlayerUtils PLAYER_UTILS = Platform.get(PlayerUtils.class);

  boolean teleportRelative(
      Player player,
      Vector deltaPos,
      float deltaYaw,
      float deltaPitch,
      PlayerTeleportEvent.TeleportCause cause);

  Skin getPlayerSkin(Player player);

  Skin getPlayerSkinForViewer(Player player, Player viewer);

  String getPlayerName(UUID uuid);

  void setAbsorption(LivingEntity entity, double health);

  double getAbsorption(LivingEntity entity);

  void showInvisibles(Player player, boolean showInvisibles);

  void setAffectsSpawning(Player player, boolean affectsSpawning);

  void setCollidesWithEntities(Player player, boolean collides);

  void setKnockbackReduction(Player player, float reduction);

  float getKnockbackReduction(Player player);

  void clearArrowsInPlayer(Player player);

  int getPing(Player player);

  void setPotionParticles(Player player, boolean enabled);

  RayBlockIntersection getTargetedBlock(Player player);

  default void sendMultiBlockPacket(
      Player player, BlockVectorSet positions, @Nullable BlockMaterialData data) {
    var location = player.getLocation();

    positions.getLongSet().forEach((long encoded) -> {
      BlockVectors.decodeInto(encoded, location);
      (data != null ? data : MaterialData.block(location.getBlock()))
          .sendBlockChange(player, location);
    });
  }
}
