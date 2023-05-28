package tc.oc.pgm.util.nms;

import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.attribute.AttributeMap;
import tc.oc.pgm.util.attribute.AttributeModifier;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.nms.entity.fake.FakeEntity;
import tc.oc.pgm.util.nms.entity.potion.EntityPotion;
import tc.oc.pgm.util.skin.Skin;

public interface NMSHacks {

  NMSHacksPlatform INSTANCE = chooseNMSHacks();

  static NMSHacksPlatform chooseNMSHacks() {
    NMSHacksPlatform choice;
    Logger logger = ClassLogger.get(NMSHacks.class);

    try {
      if (BukkitUtils.isSportPaper()) {
        choice = new NMSHacksSportPaper();
        logger.info("Using NMSHacksSportPaper");
      } else {
        choice = new NMSHacks1_8();
        logger.info("Using NMSHacks1_8");
      }
    } catch (Throwable throwable) {
      logger.severe("You are trying to run PGM on an unsupported version!");
      throw throwable;
    }
    return choice;
  }

  AtomicInteger ENTITY_IDS = new AtomicInteger(Integer.MAX_VALUE);

  static int allocateEntityId() {
    return ENTITY_IDS.decrementAndGet();
  }

  static void sendPacket(Player bukkitPlayer, Object packet) {
    INSTANCE.sendPacket(bukkitPlayer, packet);
  }

  static void playDeathAnimation(Player player) {
    INSTANCE.playDeathAnimation(player);
  }

  static Object teleportEntityPacket(int entityId, Location location) {
    return INSTANCE.teleportEntityPacket(entityId, location);
  }

  static Object entityMetadataPacket(int entityId, Entity entity, boolean complete) {
    return INSTANCE.entityMetadataPacket(entityId, entity, complete);
  }

  static void skipFireworksLaunch(Firework firework) {
    INSTANCE.skipFireworksLaunch(firework);
  }

  static void fakePlayerItemPickup(Player player, Item item) {
    INSTANCE.fakePlayerItemPickup(player, item);
  }

  static boolean isCraftItemArrowEntity(org.bukkit.entity.Item item) {
    return INSTANCE.isCraftItemArrowEntity(item);
  }

  static void freezeEntity(Entity entity) {
    INSTANCE.freezeEntity(entity);
  }

  static void setFireballDirection(Fireball entity, Vector direction) {
    INSTANCE.setFireballDirection(entity, direction);
  }

  static void removeAndAddAllTabPlayers(Player viewer) {
    INSTANCE.removeAndAddAllTabPlayers(viewer);
  }

  static void sendLegacyWearing(Player player, int slot, ItemStack item) {
    INSTANCE.sendLegacyWearing(player, slot, item);
  }

  static EntityPotion entityPotion(Location location, ItemStack potionItem) {
    return INSTANCE.entityPotion(location, potionItem);
  }

  static void sendBlockChange(Location loc, Player player, @Nullable Material material) {
    INSTANCE.sendBlockChange(loc, player, material);
  }

  static void updateChunkSnapshot(ChunkSnapshot snapshot, org.bukkit.block.BlockState blockState) {
    INSTANCE.updateChunkSnapshot(snapshot, blockState);
  }

  static void setKnockbackReduction(Player player, float amount) {
    INSTANCE.setKnockbackReduction(player, amount);
  }

  static void showInvisibles(Player player, boolean showInvisibles) {
    INSTANCE.showInvisibles(player, showInvisibles);
  }

  static void setAffectsSpawning(Player player, boolean affectsSpawning) {
    INSTANCE.setAffectsSpawning(player, affectsSpawning);
  }

  static void clearArrowsInPlayer(Player player) {
    INSTANCE.clearArrowsInPlayer(player);
  }

  static void showBorderWarning(Player player, boolean show) {
    INSTANCE.showBorderWarning(player, show);
  }

  static PotionEffectType getPotionEffectType(String key) {
    return INSTANCE.getPotionEffectType(key);
  }

  static org.bukkit.enchantments.Enchantment getEnchantment(String key) {
    return INSTANCE.getEnchantment(key);
  }

  static long getMonotonicTime(World world) {
    return INSTANCE.getMonotonicTime(world);
  }

  static int getPing(Player player) {
    return INSTANCE.getPing(player);
  }

  static Object entityEquipmentPacket(int entityId, int slot, ItemStack armor) {
    return INSTANCE.entityEquipmentPacket(entityId, slot, armor);
  }

  static void entityAttach(Player player, int entityID, int vehicleID, boolean leash) {
    INSTANCE.entityAttach(player, entityID, vehicleID, leash);
  }

  static void resumeServer() {
    INSTANCE.resumeServer();
  }

  static Inventory createFakeInventory(Player viewer, Inventory realInventory) {
    return INSTANCE.createFakeInventory(viewer, realInventory);
  }

  static FakeEntity fakeWitherSkull(World world) {
    return INSTANCE.fakeWitherSkull(world);
  }

  static FakeEntity fakeArmorStand(World world, ItemStack head) {
    return INSTANCE.fakeArmorStand(world, head);
  }

  static Set<Block> getBlocks(Chunk bukkitChunk, Material material) {
    return INSTANCE.getBlocks(bukkitChunk, material);
  }

  static Object spawnPlayerPacket(int entityId, UUID uuid, Location location, Player player) {
    return INSTANCE.spawnPlayerPacket(entityId, uuid, location, player);
  }

  static Object destroyEntitiesPacket(int... entityIds) {
    return INSTANCE.destroyEntitiesPacket(entityIds);
  }

  static Object createPlayerInfoPacket(EnumPlayerInfoAction action) {
    return INSTANCE.createPlayerInfoPacket(action);
  }

  static void setPotionParticles(Player player, boolean enabled) {
    INSTANCE.setPotionParticles(player, enabled);
  }

  static ItemStack craftItemCopy(ItemStack item) {
    return INSTANCE.craftItemCopy(item);
  }

  static RayBlockIntersection getTargetedBLock(Player player) {
    return INSTANCE.getTargetedBLock(player);
  }

  static boolean playerInfoDataListNotEmpty(Object packet) {
    return INSTANCE.playerInfoDataListNotEmpty(packet);
  }

  static Object playerListPacketData(
      Object packetPlayOutPlayerInfo,
      UUID uuid,
      String name,
      GameMode gamemode,
      int ping,
      @Nullable Skin skin,
      @Nullable String renderedDisplayName) {
    return INSTANCE.playerListPacketData(
        packetPlayOutPlayerInfo, uuid, name, gamemode, ping, skin, renderedDisplayName);
  }

  static void addPlayerInfoToPacket(Object packet, UUID uuid, int ping) {
    INSTANCE.addPlayerInfoToPacket(
        packet,
        playerListPacketData(
            packet, uuid, uuid.toString().substring(0, 16), null, ping, null, null));
  }

  static void addPlayerInfoToPacket(Object packet, UUID uuid) {
    INSTANCE.addPlayerInfoToPacket(
        packet, playerListPacketData(packet, uuid, null, null, 0, null, null));
  }

  static void addPlayerInfoToPacket(Object packet, UUID uuid, String renderedDisplayName) {
    INSTANCE.addPlayerInfoToPacket(
        packet,
        playerListPacketData(
            packet,
            uuid,
            "|" + uuid.toString().substring(0, 15),
            null,
            0,
            null,
            renderedDisplayName));
  }

  static void addPlayerInfoToPacket(
      Object packetPlayOutPlayerInfo,
      UUID uuid,
      String name,
      GameMode gamemode,
      int ping,
      @Nullable Skin skin,
      @Nullable String renderedDisplayName) {
    INSTANCE.addPlayerInfoToPacket(
        packetPlayOutPlayerInfo,
        playerListPacketData(
            packetPlayOutPlayerInfo, uuid, name, gamemode, ping, skin, renderedDisplayName));
  }

  static void setSkullMetaOwner(SkullMeta meta, String name, UUID uuid, Skin skin) {
    INSTANCE.setSkullMetaOwner(meta, name, uuid, skin);
  }

  static WorldCreator detectWorld(String worldName) {
    return INSTANCE.detectWorld(worldName);
  }

  static void setAbsorption(LivingEntity entity, double health) {
    INSTANCE.setAbsorption(entity, health);
  }

  static double getAbsorption(LivingEntity entity) {
    return INSTANCE.getAbsorption(entity);
  }

  @Deprecated
  static Set<MaterialData> getBlockStates(Material material) {
    return INSTANCE.getBlockStates(material);
  }

  static void setBlockStateData(BlockState state, MaterialData materialData) {
    INSTANCE.setBlockStateData(state, materialData);
  }

  static Skin getPlayerSkin(Player player) {
    return INSTANCE.getPlayerSkin(player);
  }

  static Skin getPlayerSkinForViewer(Player player, Player viewer) {
    return INSTANCE.getPlayerSkinForViewer(player, viewer);
  }

  static void updateVelocity(Player player) {
    INSTANCE.updateVelocity(player);
  }

  static boolean teleportRelative(
      Player player,
      org.bukkit.util.Vector deltaPos,
      float deltaYaw,
      float deltaPitch,
      PlayerTeleportEvent.TeleportCause cause) {
    return INSTANCE.teleportRelative(player, deltaPos, deltaYaw, deltaPitch, cause);
  }

  static void spawnFreezeEntity(Player player, int entityId, boolean legacy) {
    INSTANCE.spawnFreezeEntity(player, entityId, legacy);
  }

  /**
   * Test if the given tool is capable of "efficiently" mining the given block.
   *
   * <p>Derived from CraftBlock.itemCausesDrops()
   */
  @Deprecated
  static boolean canMineBlock(MaterialData blockMaterial, ItemStack tool) {
    return INSTANCE.canMineBlock(blockMaterial, tool);
  }

  static void resetDimension(World world) {
    INSTANCE.resetDimension(world);
  }

  static void setCanDestroy(ItemMeta itemMeta, Collection<Material> materials) {
    INSTANCE.setCanDestroy(itemMeta, materials);
  }

  static Set<Material> getCanDestroy(ItemMeta itemMeta) {
    return INSTANCE.getCanDestroy(itemMeta);
  }

  static void setCanPlaceOn(ItemMeta itemMeta, Collection<Material> materials) {
    INSTANCE.setCanPlaceOn(itemMeta, materials);
  }

  static Set<Material> getCanPlaceOn(ItemMeta itemMeta) {
    return INSTANCE.getCanPlaceOn(itemMeta);
  }

  static void copyAttributeModifiers(ItemMeta destination, ItemMeta source) {
    INSTANCE.copyAttributeModifiers(destination, source);
  }

  static void applyAttributeModifiers(
      SetMultimap<String, AttributeModifier> attributeModifiers, ItemMeta meta) {
    INSTANCE.applyAttributeModifiers(attributeModifiers, meta);
  }

  static double getTPS() {
    return INSTANCE.getTPS();
  }

  static void sendDestroyTeamDummyPacket() {
    Object packet = teamRemovePacket("dummy");
    for (Player pl : Bukkit.getOnlinePlayers()) {
      sendPacket(pl, packet);
    }
  }

  static Object teamPacket(
      int operation,
      String name,
      String displayName,
      String prefix,
      String suffix,
      boolean friendlyFire,
      boolean seeFriendlyInvisibles,
      NameTagVisibility nameTagVisibility,
      Collection<String> players) {
    return INSTANCE.teamPacket(
        operation,
        name,
        displayName,
        prefix,
        suffix,
        friendlyFire,
        seeFriendlyInvisibles,
        nameTagVisibility,
        players);
  }

  static Object teamCreatePacket(
      String name,
      String displayName,
      String prefix,
      String suffix,
      boolean friendlyFire,
      boolean seeFriendlyInvisibles,
      Collection<String> players) {
    return teamPacket(
        0,
        name,
        displayName,
        prefix,
        suffix,
        friendlyFire,
        seeFriendlyInvisibles,
        NameTagVisibility.ALWAYS,
        players);
  }

  static Object teamRemovePacket(String name) {
    return teamPacket(1, name, null, null, null, false, false, null, Lists.<String>newArrayList());
  }

  static Object teamUpdatePacket(
      String name,
      String displayName,
      String prefix,
      String suffix,
      boolean friendlyFire,
      boolean seeFriendlyInvisibles) {
    return teamPacket(
        2,
        name,
        displayName,
        prefix,
        suffix,
        friendlyFire,
        seeFriendlyInvisibles,
        NameTagVisibility.ALWAYS,
        Lists.newArrayList());
  }

  static Object teamJoinPacket(String name, Collection<String> players) {
    return teamPacket(3, name, null, null, null, false, false, null, players);
  }

  static Object teamLeavePacket(String name, Collection<String> players) {
    return teamPacket(4, name, null, null, null, false, false, null, players);
  }

  static AttributeMap buildAttributeMap(Player player) {
    return INSTANCE.buildAttributeMap(player);
  }

  static void postToMainThread(Plugin plugin, boolean priority, Runnable task) {
    INSTANCE.postToMainThread(plugin, priority, task);
  }
}
