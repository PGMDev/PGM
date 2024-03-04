package tc.oc.pgm.util.nms;

import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.DyeColor;
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
import tc.oc.pgm.util.attribute.AttributeMap;
import tc.oc.pgm.util.attribute.AttributeModifier;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.util.nms.entity.fake.FakeEntity;
import tc.oc.pgm.util.nms.entity.potion.EntityPotion;
import tc.oc.pgm.util.skin.Skin;

public interface NMSHacksPlatform {

  void sendPacket(Player player, Object packet);

  void sendPacketToViewers(Entity entity, Object packet, boolean excludeSpectators);

  void playDeathAnimation(Player player);

  Object teleportEntityPacket(int entityId, Location location);

  Object entityMetadataPacket(int entityId, Entity entity, boolean complete);

  void skipFireworksLaunch(Firework firework);

  void fakePlayerItemPickup(Player player, Item item);

  boolean isCraftItemArrowEntity(org.bukkit.entity.Item item);

  void freezeEntity(Entity entity);

  void setFireballDirection(Fireball entity, Vector direction);

  void removeAndAddAllTabPlayers(Player viewer);

  void sendLegacyWearing(Player player, int slot, ItemStack item);

  EntityPotion entityPotion(Location location, ItemStack potionItem);

  void sendBlockChange(Location loc, Player player, @Nullable Material material);

  default void updateChunkSnapshot(
      ChunkSnapshot snapshot, org.bukkit.block.BlockState blockState) {}

  default void setKnockbackReduction(Player player, float amount) {}

  default void showInvisibles(Player player, boolean showInvisibles) {}

  default void setAffectsSpawning(Player player, boolean affectsSpawning) {}

  void clearArrowsInPlayer(Player player);

  void showBorderWarning(Player player, boolean show);

  PotionEffectType getPotionEffectType(String key);

  org.bukkit.enchantments.Enchantment getEnchantment(String key);

  long getMonotonicTime(World world);

  int getPing(Player player);

  Object entityEquipmentPacket(int entityId, int slot, ItemStack armor);

  void entityAttach(Player player, int entityID, int vehicleID, boolean leash);

  default void resumeServer() {}

  Inventory createFakeInventory(Player viewer, Inventory realInventory);

  FakeEntity fakeWitherSkull(World world);

  FakeEntity fakeArmorStand(World world, ItemStack head);

  Set<Block> getBlocks(Chunk bukkitChunk, Material material);

  Object spawnPlayerPacket(int entityId, UUID uuid, Location location, Player player);

  Object destroyEntitiesPacket(int... entityIds);

  Object createPlayerInfoPacket(EnumPlayerInfoAction action);

  void setPotionParticles(Player player, boolean enabled);

  ItemStack craftItemCopy(ItemStack item);

  RayBlockIntersection getTargetedBLock(Player player);

  boolean playerInfoDataListNotEmpty(Object packet);

  Object playerListPacketData(
      Object packetPlayOutPlayerInfo,
      UUID uuid,
      String name,
      GameMode gamemode,
      int ping,
      @Nullable Skin skin,
      @Nullable String renderedDisplayName);

  void addPlayerInfoToPacket(Object packet, Object playerInfoData);

  void setSkullMetaOwner(SkullMeta meta, String name, UUID uuid, Skin skin);

  WorldCreator detectWorld(String worldName);

  void setAbsorption(LivingEntity entity, double health);

  double getAbsorption(LivingEntity entity);

  @Deprecated
  Set<MaterialData> getBlockStates(Material material);

  // TODO: Material api
  void setBlockStateData(BlockState state, MaterialData materialData);

  Skin getPlayerSkin(Player player);

  Skin getPlayerSkinForViewer(Player player, Player viewer);

  void updateVelocity(Player player);

  boolean teleportRelative(
      Player player,
      org.bukkit.util.Vector deltaPos,
      float deltaYaw,
      float deltaPitch,
      PlayerTeleportEvent.TeleportCause cause);

  void sendSpawnEntityPacket(Player player, int entityId, Location location, Vector velocity);

  default void sendSpawnEntityPacket(Player player, int entityId, Location location) {
    sendSpawnEntityPacket(player, entityId, location, new Vector());
  }

  void spawnFreezeEntity(Player player, int entityId, boolean legacy);

  void spawnFakeArmorStand(Player player, int entityId, Location location, Vector velocity);

  /**
   * Test if the given tool is capable of "efficiently" mining the given block.
   *
   * <p>Derived from CraftBlock.itemCausesDrops()
   */
  boolean canMineBlock(MaterialData blockMaterial, ItemStack tool);

  void resetDimension(World world);

  Set<Material> getMaterialCollection(ItemMeta itemMeta, String key);

  void setMaterialCollection(ItemMeta itemMeta, Collection<Material> materials, String canPlaceOn);

  void setCanDestroy(ItemMeta itemMeta, Collection<Material> materials);

  Set<Material> getCanDestroy(ItemMeta itemMeta);

  void setCanPlaceOn(ItemMeta itemMeta, Collection<Material> materials);

  Set<Material> getCanPlaceOn(ItemMeta itemMeta);

  void copyAttributeModifiers(ItemMeta destination, ItemMeta source);

  void applyAttributeModifiers(
      SetMultimap<String, AttributeModifier> attributeModifiers, ItemMeta meta);

  SetMultimap<String, AttributeModifier> getAttributeModifiers(ItemMeta meta);

  double getTPS();

  Object teamPacket(
      int operation,
      String name,
      String displayName,
      String prefix,
      String suffix,
      boolean friendlyFire,
      boolean seeFriendlyInvisibles,
      NameTagVisibility nameTagVisibility,
      Collection<String> players);

  AttributeMap buildAttributeMap(Player player);

  void postToMainThread(Plugin plugin, boolean priority, Runnable task);

  void showPayloadParticles(World world, Location loc, Color color);

  void playBreakEffect(Location location, MaterialData material);

  void showSpawnedFlagParticles(Player player, Location location, DyeColor flagDyeColor);

  void showBlitzSmoke(World world, Location base, int j);

  void showCriticalArrowParticles(Player player, Location projectileLocation);

  void showColoredArrowParticles(Player player, Location projectileLocation, Color color);

  void showSpawnerFlameParticles(World world, Location location);

  void showHugeExplosionParticle(Player player, Location explosion);
}
