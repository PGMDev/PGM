package tc.oc.pgm.platform.modern.impl;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
import tc.oc.pgm.platform.modern.util.Skins;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.nms.PlayerUtils;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.skin.Skin;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernPlayerUtils implements PlayerUtils {

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
  public void showInvisibles(Player player, boolean showInvisibles) {
    // TODO: PLATFORM 1.20 does not support allowing seeing invisible players
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

  public static final String HIDE_PARTICLES = "hideParticles";
  private static final FixedMetadataValue TRUE =
      new FixedMetadataValue(BukkitUtils.getPlugin(), true);

  @Override
  public void setPotionParticles(Player player, boolean enabled) {
    if (player.hasMetadata(HIDE_PARTICLES) != enabled) return;

    if (enabled) player.removeMetadata(HIDE_PARTICLES, BukkitUtils.getPlugin());
    else player.setMetadata(HIDE_PARTICLES, TRUE);

    var nmsPlayer = ((CraftPlayer) player).getHandle();
    if (!nmsPlayer.getActiveEffects().isEmpty()) {
      nmsPlayer.effectsDirty = true;
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
}
