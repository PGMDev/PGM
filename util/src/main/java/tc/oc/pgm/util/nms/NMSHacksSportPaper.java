package tc.oc.pgm.util.nms;

import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.PacketPlayOutAttachEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.scoreboard.CraftTeam;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.pgm.util.attribute.AttributeModifier;
import tc.oc.pgm.util.skin.Skin;

public class NMSHacksSportPaper extends NMSHacks1_8 {
  @Override
  public void updateChunkSnapshot(ChunkSnapshot snapshot, BlockState blockState) {
    snapshot.updateBlock(blockState);
  }

  @Override
  public void setBlockStateData(BlockState state, MaterialData materialData) {
    state.setMaterialData(materialData);
  }

  @Override
  public void setKnockbackReduction(Player player, float amount) {
    player.setKnockbackReduction(amount);
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
  public void entityAttach(Player player, int entityID, int vehicleID, boolean leash) {
    sendPacket(player, new PacketPlayOutAttachEntity(entityID, vehicleID, leash));
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
  public Object spawnPlayerPacket(int entityId, UUID uuid, Location location, Player player) {
    DataWatcher dataWatcher = copyDataWatcher(player);
    return new PacketPlayOutNamedEntitySpawn(
        entityId,
        uuid,
        location.getX(),
        location.getY(),
        location.getZ(),
        (byte) location.getYaw(),
        (byte) location.getPitch(),
        CraftItemStack.asNMSCopy(null),
        dataWatcher);
  }

  @Override
  public Object createPlayerInfoPacket(EnumPlayerInfoAction action) {
    PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
    packet.a = convertEnumPlayerInfoAction(action);
    return packet;
  }

  @Override
  public void setPotionParticles(Player player, boolean enabled) {
    player.setPotionParticles(enabled);
  }

  @Override
  public boolean playerInfoDataListNotEmpty(Object packet) {
    return !((PacketPlayOutPlayerInfo) packet).b.isEmpty();
  }

  @Override
  public void addPlayerInfoToPacket(Object packet, Object playerInfoData) {
    ((PacketPlayOutPlayerInfo) packet)
        .b.add((PacketPlayOutPlayerInfo.PlayerInfoData) playerInfoData);
  }

  @Override
  public void sendSpawnEntityPacket(Player player, int entityId, Location location) {
    sendPacket(
        player,
        new PacketPlayOutSpawnEntity(
            entityId,
            location.getX(),
            location.getY(),
            location.getZ(),
            0,
            0,
            0,
            (int) location.getPitch(),
            (int) location.getYaw(),
            66,
            0));
  }

  @Override
  public void spawnFreezeEntity(Player player, int entityId, boolean legacy) {
    if (legacy) {
      Location location = player.getLocation().add(0, 0.286, 0);
      if (location.getY() < -64) {
        location.setY(-64);
        player.teleport(location);
      }

      sendSpawnEntityPacket(player, entityId, location);
    } else {
      Location loc = player.getLocation().subtract(0, 1.1, 0);
      int flags = 0;
      flags |= 0x20;
      DataWatcher dataWatcher = new DataWatcher(null);
      dataWatcher.a(0, (byte) (byte) flags);
      dataWatcher.a(1, (short) (short) 0);
      int flags1 = 0;
      dataWatcher.a(10, (byte) flags1);
      sendPacket(
          player,
          new PacketPlayOutSpawnEntityLiving(
              entityId,
              (byte) EntityType.ARMOR_STAND.getTypeId(),
              loc.getX(),
              loc.getY(),
              loc.getZ(),
              loc.getYaw(),
              loc.getPitch(),
              loc.getPitch(),
              0,
              0,
              0,
              dataWatcher));
    }
  }

  @Override
  public Skin getPlayerSkinForViewer(Player player, Player viewer) {
    return player.hasFakeSkin(viewer)
        ? new Skin(player.getFakeSkin(viewer).getData(), player.getFakeSkin(viewer).getSignature())
        : getPlayerSkin(player);
  }

  @Override
  public void setCanDestroy(ItemMeta itemMeta, Collection<Material> materials) {
    itemMeta.setCanDestroy(materials);
  }

  @Override
  public Set<Material> getCanDestroy(ItemMeta itemMeta) {
    return itemMeta.getCanDestroy();
  }

  @Override
  public void setCanPlaceOn(ItemMeta itemMeta, Collection<Material> materials) {
    itemMeta.setCanPlaceOn(materials);
  }

  @Override
  public Set<Material> getCanPlaceOn(ItemMeta itemMeta) {
    return itemMeta.getCanPlaceOn();
  }

  @Override
  public void copyAttributeModifiers(ItemMeta destination, ItemMeta source) {
    for (String attribute : source.getModifiedAttributes()) {
      for (org.bukkit.attribute.AttributeModifier modifier :
          source.getAttributeModifiers(attribute)) {
        destination.addAttributeModifier(attribute, modifier);
      }
    }
  }

  @Override
  public void applyAttributeModifiers(
      SetMultimap<String, AttributeModifier> attributeModifiers, ItemMeta meta) {
    for (Map.Entry<String, AttributeModifier> entry : attributeModifiers.entries()) {
      AttributeModifier attributeModifier = entry.getValue();
      meta.addAttributeModifier(
          entry.getKey(),
          new org.bukkit.attribute.AttributeModifier(
              attributeModifier.getUniqueId(),
              attributeModifier.getName(),
              attributeModifier.getAmount(),
              org.bukkit.attribute.AttributeModifier.Operation.fromOpcode(
                  attributeModifier.getOperation().ordinal())));
    }
  }

  @Override
  public double getTPS() {
    return Bukkit.getServer().spigot().getTPS()[0];
  }

  @Override
  public Object teamPacket(
      int operation,
      String name,
      String displayName,
      String prefix,
      String suffix,
      boolean friendlyFire,
      boolean seeFriendlyInvisibles,
      NameTagVisibility nameTagVisibility,
      Collection<String> players) {
    PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();

    packet.a = name;
    packet.b = displayName;
    packet.c = prefix;
    packet.d = suffix;
    packet.e = nameTagVisibility == null ? null : CraftTeam.bukkitToNotch(nameTagVisibility).e;
    // packet.f = color
    packet.g = players;
    packet.h = operation;
    if (friendlyFire) {
      packet.i |= 1;
    }
    if (seeFriendlyInvisibles) {
      packet.i |= 2;
    }

    return packet;
  }

  @Override
  public void postToMainThread(Plugin plugin, boolean priority, Runnable task) {
    Bukkit.getServer().postToMainThread(plugin, true, task);
  }
}
