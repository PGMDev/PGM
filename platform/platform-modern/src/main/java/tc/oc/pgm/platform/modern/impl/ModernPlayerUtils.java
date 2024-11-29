package tc.oc.pgm.platform.modern.impl;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import tc.oc.pgm.platform.modern.packets.PacketManipulations;
import tc.oc.pgm.platform.modern.util.Skins;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.nms.PlayerUtils;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.skin.Skin;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernPlayerUtils implements PlayerUtils {

  private static final FixedMetadataValue TRUE =
      new FixedMetadataValue(BukkitUtils.getPlugin(), true);

  @Override
  public boolean teleportRelative(
      Player player,
      Vector deltaPos,
      float deltaYaw,
      float deltaPitch,
      PlayerTeleportEvent.TeleportCause cause) {
    Location result = player.getLocation().clone().add(deltaPos);
    result.setYaw(result.getYaw() + deltaYaw);
    result.setPitch(result.getPitch() + deltaPitch);
    return player.teleport(result, cause);
  }

  @Override
  public Skin getPlayerSkin(Player player) {
    CraftPlayer craftPlayer = (CraftPlayer) player;
    return Skins.fromProfile(craftPlayer.getProfile());
  }

  @Override
  public Skin getPlayerSkinForViewer(Player player, Player viewer) {
    // No support for viewer-specific skins
    return getPlayerSkin(player);
  }

  @Override
  public String getPlayerName(UUID uuid) {
    return Optional.ofNullable(MinecraftServer.getServer().getProfileCache())
        .flatMap(c -> c.get(uuid).map(GameProfile::getName))
        .orElse(null);
  }

  @Override
  public void setAbsorption(LivingEntity entity, double health) {
    entity.setAbsorptionAmount((float) health);
  }

  @Override
  public double getAbsorption(LivingEntity entity) {
    return entity.getAbsorptionAmount();
  }

  @Override
  public void showInvisibles(Player player, boolean showInvisible) {
    if (updateMetadata(player, showInvisible, PacketManipulations.SHOW_INVISIBLE_KEY)) {
      // Refresh all seen entities' metadata
      var nmsPlayer = ((CraftPlayer) player).getHandle();
      ServerLevel world = (ServerLevel) nmsPlayer.level();
      var entityMap = world.getChunkSource().chunkMap.entityMap;

      for (var entity : world.players()) {
        if (entity.isInvisible() && player.canSee(entity.getBukkitEntity())) {
          var trackedEntity = entityMap.get(entity.getId());
          if (trackedEntity != null && trackedEntity.seenBy.contains(nmsPlayer.connection)) {
            entity.refreshEntityData(nmsPlayer);
          }
        }
      }
    }
  }

  @Override
  public void setAffectsSpawning(Player player, boolean affectsSpawning) {
    player.setAffectsSpawning(affectsSpawning);
  }

  @Override
  public void setCollidesWithEntities(Player player, boolean collides) {
    player.setCollidable(collides);
  }

  private final ResourceLocation KB_REDUCT =
      ResourceLocation.fromNamespaceAndPath("pgm", "custom_kb_reduction");

  @Override
  public void setKnockbackReduction(Player player, float reduction) {
    // Use NMS to have access to addOrUpdateTransientModifier
    var nmsPlayer = ((CraftPlayer) player).getHandle();
    var attr = nmsPlayer.getAttributes().getInstance(Attributes.KNOCKBACK_RESISTANCE);
    if (attr == null) return;
    attr.addOrUpdateTransientModifier(
        new AttributeModifier(KB_REDUCT, reduction, AttributeModifier.Operation.ADD_VALUE));
  }

  @Override
  public float getKnockbackReduction(Player player) {
    var attr = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
    return attr != null ? (float) attr.getValue() : 0;
  }

  @Override
  public void clearArrowsInPlayer(Player player) {
    player.setArrowsInBody(0);
  }

  @Override
  public int getPing(Player player) {
    return player.getPing();
  }

  @Override
  public void setPotionParticles(Player player, boolean enabled) {
    if (updateMetadata(player, !enabled, PacketManipulations.HIDE_PARTICLES_KEY)) {
      var nmsPlayer = ((CraftPlayer) player).getHandle();
      if (!nmsPlayer.getActiveEffects().isEmpty()) {
        nmsPlayer.effectsDirty = true;
      }
    }
  }

  @Override
  public RayBlockIntersection getTargetedBlock(Player player) {
    var attr = player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE);
    double range = attr == null ? 3.5 : attr.getValue();
    var result = player.rayTraceBlocks(range);
    if (result != null) {
      return new RayBlockIntersection(
          result.getHitBlock(), result.getHitBlockFace(), result.getHitPosition());
    } else {
      return null;
    }
  }

  private boolean updateMetadata(Player player, boolean set, String key) {
    if (player.hasMetadata(key)) return false;

    if (set) player.setMetadata(key, TRUE);
    else player.removeMetadata(key, BukkitUtils.getPlugin());
    return true;
  }
}
