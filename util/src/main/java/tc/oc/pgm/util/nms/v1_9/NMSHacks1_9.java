package tc.oc.pgm.util.nms.v1_9;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.attribute.AttributeMap;
import tc.oc.pgm.util.attribute.AttributeModifier;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.nms.EnumPlayerInfoAction;
import tc.oc.pgm.util.nms.NMSHacksNoOp;
import tc.oc.pgm.util.nms.attribute.AttributeMapBukkit;
import tc.oc.pgm.util.nms.entity.fake.FakeEntity;
import tc.oc.pgm.util.nms.entity.fake.armorstand.FakeArmorStandProtocolLib;
import tc.oc.pgm.util.nms.entity.fake.wither.FakeWitherSkullProtocolLib;
import tc.oc.pgm.util.nms.reflect.Refl;
import tc.oc.pgm.util.nms.reflect.ReflectionProxy;
import tc.oc.pgm.util.skin.Skin;

public class NMSHacks1_9 extends NMSHacksNoOp {

  static ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

  static Refl refl = ReflectionProxy.getProxy(Refl.class);
  static Refl.NBTTagString nbtTagString = ReflectionProxy.getProxy(Refl.NBTTagString.class);
  static Refl.NBTTagList nbtTagList = ReflectionProxy.getProxy(Refl.NBTTagList.class);
  static Refl.NBTTagCompound nbtTagCompound = ReflectionProxy.getProxy(Refl.NBTTagCompound.class);
  static Refl.CraftMagicNumbers craftMagicNumbers =
      ReflectionProxy.getProxy(Refl.CraftMagicNumbers.class);
  static Refl.Block reflBlock = ReflectionProxy.getProxy(Refl.Block.class);
  static Refl.CraftItemStack craftItemStack = ReflectionProxy.getProxy(Refl.CraftItemStack.class);

  @Override
  public Set<Material> getMaterialCollection(ItemMeta itemMeta, String key) {
    Map<String, Object> unhandledTags = refl.getUnhandledTags(itemMeta);
    if (!unhandledTags.containsKey(key)) return EnumSet.noneOf(Material.class);
    EnumSet<Material> materialSet = EnumSet.noneOf(Material.class);
    Object canDestroyList = unhandledTags.get(key);

    for (Object item : (List<Object>) nbtTagList.getListField(canDestroyList)) {
      String blockString = nbtTagString.getString(item);
      Object nmsBlock = reflBlock.getBlockByName(blockString);
      int id = reflBlock.getId(nmsBlock);
      Material material = Material.getMaterial(id);
      materialSet.add(material);
    }

    return materialSet;
  }

  @Override
  public void setMaterialCollection(
      ItemMeta itemMeta, Collection<Material> materials, String collectionName) {
    Map<String, Object> unhandledTags = refl.getUnhandledTags(itemMeta);
    Object canDestroyList =
        unhandledTags.containsKey(collectionName)
            ? unhandledTags.get(collectionName)
            : nbtTagList.build();
    for (Material material : materials) {
      Object block = craftMagicNumbers.getBlock(material);

      if (block != null) {
        String blockString = block.toString(); // Format: Block{what we want}
        blockString = blockString.substring(6, blockString.length() - 1);
        Object nbtString = nbtTagString.build(blockString);
        nbtTagList.add(canDestroyList, nbtString);
      }
    }
    if (!nbtTagList.isEmpty(canDestroyList)) unhandledTags.put(collectionName, canDestroyList);
  }

  @Override
  public boolean canMineBlock(Material material, ItemStack tool) {
    if (!material.isBlock()) {
      throw new IllegalArgumentException("Material '" + material + "' is not a block");
    }

    Object nmsBlock = craftMagicNumbers.getBlock(material);
    Object nmsTool = tool == null ? null : craftMagicNumbers.getItem(tool.getType());

    Object iBlockData = reflBlock.getBlockData(nmsBlock);

    boolean alwaysDestroyable = refl.isAlwaysDestroyable(reflBlock.getMaterial(nmsBlock));
    boolean toolCanDestroy = nmsTool != null && refl.canDestroySpecialBlock(nmsTool, iBlockData);
    return nmsBlock != null && (alwaysDestroyable || toolCanDestroy);
  }

  @Override
  public AttributeMap buildAttributeMap(Player player) {
    return new AttributeMapBukkit(player);
  }

  @Override
  public void applyAttributeModifiers(
      SetMultimap<String, AttributeModifier> attributeModifiers, ItemMeta meta) {
    Object list = nbtTagList.build();
    if (!attributeModifiers.isEmpty()) {
      for (Map.Entry<String, AttributeModifier> entry : attributeModifiers.entries()) {
        AttributeModifier modifier = entry.getValue();
        Object tag = nbtTagCompound.build();
        nbtTagCompound.setString(tag, "Name", modifier.getName());
        nbtTagCompound.setDouble(tag, "Amount", modifier.getAmount());
        nbtTagCompound.setInt(tag, "Operation", modifier.getOperation().ordinal());
        nbtTagCompound.setLong(tag, "UUIDMost", modifier.getUniqueId().getMostSignificantBits());
        nbtTagCompound.setLong(tag, "UUIDLeast", modifier.getUniqueId().getLeastSignificantBits());
        nbtTagCompound.setString(tag, "AttributeName", entry.getKey());
        nbtTagList.add(list, tag);
      }

      Map<String, Object> unhandledTags = refl.getUnhandledTags(meta);

      unhandledTags.put("AttributeModifiers", list);
    }
  }

  @Override
  public SetMultimap<String, AttributeModifier> getAttributeModifiers(ItemMeta meta) {
    Map<String, Object> unhandledTags = refl.getUnhandledTags(meta);
    if (unhandledTags.containsKey("AttributeModifiers")) {
      final SetMultimap<String, AttributeModifier> attributeModifiers = HashMultimap.create();
      final Object modTags = unhandledTags.get("AttributeModifiers");
      for (int i = 0; i < nbtTagList.size(modTags); i++) {
        final Object modTag = nbtTagList.get(modTags, i);
        attributeModifiers.put(
            nbtTagCompound.getString(modTag, "AttributeName"),
            new AttributeModifier(
                new UUID(
                    nbtTagCompound.getLong(modTag, "UUIDMost"),
                    nbtTagCompound.getLong(modTag, "UUIDLeast")),
                nbtTagCompound.getString(modTag, "Name"),
                nbtTagCompound.getDouble(modTag, "Amount"),
                AttributeModifier.Operation.fromOpcode(
                    nbtTagCompound.getInt(modTag, "Operation"))));
      }
      return attributeModifiers;
    } else {
      return HashMultimap.create();
    }
  }

  @Override
  public ItemStack craftItemCopy(ItemStack item) {
    return craftItemStack.asCraftCopy(item);
  }

  @Override
  public void postToMainThread(Plugin plugin, boolean priority, Runnable task) {
    Server bukkitServer = plugin.getServer();
    Object nmsServer = refl.getNMSServer(refl.getCraftServerHandle(bukkitServer));
    refl.addCallableToMainThread(nmsServer, Executors.callable(task));
  }

  @Override
  public void sendPacket(Player bukkitPlayer, Object packet) {
    if (packet != null && bukkitPlayer != null) {
      protocolManager.sendServerPacket(bukkitPlayer, (PacketContainer) packet);
    }
  }

  private List<Player> getViewingPlayers(Entity entity) {
    // TODO: use radius defined in spigot.yml / sportpaper.yml
    List<Player> players = new ArrayList<>();
    for (Entity nearbyEntity :
        entity.getWorld().getNearbyEntities(entity.getLocation(), 64, 64, 64)) {
      if (nearbyEntity instanceof Player && nearbyEntity.getEntityId() != entity.getEntityId()) {
        players.add((Player) nearbyEntity);
      }
    }
    return players;
  }

  @Override
  public void sendPacketToViewers(Entity entity, Object packet, boolean excludeSpectators) {
    for (Player nearbyPlayer : getViewingPlayers(entity)) {
      if (excludeSpectators) {
        Entity spectatorTarget = nearbyPlayer.getSpectatorTarget();
        if (spectatorTarget != null && spectatorTarget.getUniqueId().equals(entity.getUniqueId()))
          continue;
      }
      sendPacket(nearbyPlayer, packet);
    }
  }

  @Override
  public void playDeathAnimation(Player player) {
    float health = 0.0f;
    PacketContainer metadataPacket = getMetadataPacket(player, health);

    sendPacketToViewers(player, metadataPacket, true);

    metadataPacket = getMetadataPacket(player, 1.0f);
    sendPacket(player, metadataPacket);

    //     Reimplement using Poses in 1.13+
    Location location = player.getLocation();
    PacketContainer useBedPacket = new PacketContainer(PacketType.Play.Server.USE_BED);
    useBedPacket.getEntityModifier(player.getWorld()).write(0, player);
    BlockPosition blockPosition =
        new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    useBedPacket.getBlockPositionModifier().write(0, blockPosition);
    sendPacketToViewers(player, useBedPacket, true);

    Object teleport = teleportEntityPacket(player.getEntityId(), location);
    sendPacketToViewers(player, teleport, true);
  }

  @NotNull
  public PacketContainer getMetadataPacket(Player player, float health) {
    PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);

    metadataPacket.getIntegers().write(0, player.getEntityId());

    WrappedDataWatcher wrappedDataWatcher = WrappedDataWatcher.getEntityWatcher(player).deepClone();

    WrappedDataWatcher.WrappedDataWatcherObject watcherObject =
        new WrappedDataWatcher.WrappedDataWatcherObject(
            6, WrappedDataWatcher.Registry.get(Float.class));
    wrappedDataWatcher.setObject(watcherObject, health);

    metadataPacket
        .getWatchableCollectionModifier()
        .write(0, wrappedDataWatcher.getWatchableObjects());
    return metadataPacket;
  }

  @Override
  public Object teleportEntityPacket(int entityId, Location location) {
    PacketContainer entityTeleportPacket =
        new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
    entityTeleportPacket.getIntegers().write(0, entityId);

    entityTeleportPacket
        .getDoubles()
        .write(0, location.getX())
        .write(1, location.getY())
        .write(2, location.getZ());

    entityTeleportPacket
        .getBytes()
        .write(0, (byte) (location.getYaw() * 256 / 360))
        .write(1, (byte) (location.getPitch() * 256 / 360));

    entityTeleportPacket.getBooleans().write(0, true);

    return entityTeleportPacket;
  }

  @Override
  public Object entityMetadataPacket(int entityId, Entity entity, boolean complete) {
    PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);

    packetContainer.getIntegers().write(0, entityId);

    WrappedDataWatcher wrappedDataWatcher = WrappedDataWatcher.getEntityWatcher(entity).deepClone();
    packetContainer.getDataWatcherModifier().write(0, wrappedDataWatcher);

    return packetContainer;
  }

  @Override
  public void skipFireworksLaunch(Firework firework) {
    NBTEditor.set(firework, "Life", 2);
    NBTEditor.set(firework, "LifeTime", 2);
    sendPacketToViewers(
        firework, entityMetadataPacket(firework.getEntityId(), firework, false), false);
  }

  static Sound itemPickupSound = Sound.valueOf("ENTITY_ITEM_PICKUP");

  @Override
  public void fakePlayerItemPickup(Player player, Item item) {
    float pitch = (((float) (Math.random() - Math.random()) * 0.7F + 1.0F) * 2.0F);
    item.getWorld().playSound(item.getLocation(), itemPickupSound, 0.2F, pitch);

    PacketContainer packet = new PacketContainer(PacketType.Play.Server.COLLECT);
    packet.getIntegers().write(0, item.getEntityId());
    packet.getIntegers().write(1, player.getEntityId());

    sendPacketToViewers(item, packet, false);

    item.remove();
  }

  @Override
  public void freezeEntity(Entity entity) {
    NBTEditor.set(entity, true, "NoAI");
    NBTEditor.set(entity, true, "NoGravity");
  }

  @Override
  public void setFireballDirection(Fireball entity, Vector direction) {
    List<Double> doubles = new ArrayList<>();
    doubles.add(direction.getX() * 0.1D);
    doubles.add(direction.getY() * 0.1D);
    doubles.add(direction.getZ() * 0.1D);
    NBTEditor.set(entity, doubles, "power");
  }

  @Override
  public void removeAndAddAllTabPlayers(Player viewer) {
    List<PlayerInfoData> playerInfoDataList = new ArrayList<>();

    for (Player player : Bukkit.getOnlinePlayers()) {
      if (viewer.canSee(player) || player == viewer) {
        playerInfoDataList.add(
            new PlayerInfoData(
                WrappedGameProfile.fromPlayer(player),
                getPing(player),
                EnumWrappers.NativeGameMode.fromBukkit(viewer.getGameMode()),
                WrappedChatComponent.fromLegacyText(player.getPlayerListName())));
      }
    }

    PacketContainer removePlayerPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
    removePlayerPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);

    removePlayerPacket.getPlayerInfoDataLists().write(0, playerInfoDataList);
    sendPacket(viewer, removePlayerPacket);

    PacketContainer addPlayerPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
    addPlayerPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);

    addPlayerPacket.getPlayerInfoDataLists().write(0, playerInfoDataList);
    sendPacket(viewer, addPlayerPacket);
  }

  @Override
  public void sendLegacyWearing(Player player, int slot, ItemStack item) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);

    packet.getIntegers().write(0, player.getEntityId());

    if (slot != 4) {
      throw new UnsupportedOperationException(
          "NMSHacks.entityEquipmentPacket needs to be refactored to support this!");
    }
    packet.getItemSlots().write(0, EnumWrappers.ItemSlot.HEAD);

    packet.getItemModifier().write(0, item);

    for (Player viewingPlayer : getViewingPlayers(player)) {
      if (ViaUtils.getProtocolVersion(viewingPlayer) <= ViaUtils.VERSION_1_7) {
        sendPacket(viewingPlayer, packet);
      }
    }
  }

  @Override
  public void sendBlockChange(Location loc, Player player, @Nullable Material material) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);

    BlockPosition blockPosition = new BlockPosition(loc.toVector());

    packet.getBlockPositionModifier().write(0, blockPosition);

    WrappedBlockData data;

    if (material == null) {
      Block block = loc.getBlock();
      data = WrappedBlockData.createData(block.getType(), block.getData());
    } else {
      data = WrappedBlockData.createData(material);
    }

    packet.getBlockData().write(0, data);

    sendPacket(player, packet);
  }

  @Override
  public void clearArrowsInPlayer(Player player) {
    WrappedDataWatcher entityWatcher = WrappedDataWatcher.getEntityWatcher(player);
    entityWatcher.setObject(9, (int) 0, true);
  }

  @Override
  public void showBorderWarning(Player player, boolean show) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.WORLD_BORDER);
    packet.getWorldBorderActions().write(0, EnumWrappers.WorldBorderAction.SET_WARNING_BLOCKS);

    packet.getIntegers().write(0, 29999984).write(1, 15).write(2, show ? Integer.MAX_VALUE : 0);

    packet.getDoubles().write(0, 0.0).write(1, 0.0).write(2, 6.0E7D).write(3, 6.0E7D);
    packet.getLongs().write(0, 0L);

    sendPacket(player, packet);
  }

  @Override
  public Object entityEquipmentPacket(int entityId, int slot, ItemStack armor) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);

    packet.getIntegers().write(0, entityId);

    if (slot != 4) {
      throw new UnsupportedOperationException(
          "NMSHacks.entityEquipmentPacket needs to be refactored to support this!");
    } else {
      packet.getItemSlots().write(0, EnumWrappers.ItemSlot.HEAD);
    }

    packet.getItemModifier().write(0, armor);

    return packet;
  }

  @Override
  public void entityAttach(Player player, int entityID, int vehicleID, boolean leash) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.MOUNT);

    packet.getIntegers().write(0, vehicleID);

    int[] ridingEntities = {entityID};

    packet.getIntegerArrays().write(0, ridingEntities);

    sendPacket(player, packet);
  }

  @Override
  public FakeEntity fakeWitherSkull(World world) {
    return new FakeWitherSkullProtocolLib();
  }

  @Override
  public FakeEntity fakeArmorStand(World world, ItemStack head) {
    return new FakeArmorStandProtocolLib(head);
  }

  @Override
  public Object spawnPlayerPacket(int entityId, UUID uuid, Location location, Player player) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);

    packet.getIntegers().write(0, entityId);

    packet
        .getDoubles()
        .write(0, location.getX())
        .write(1, location.getY())
        .write(2, location.getZ());

    packet.getUUIDs().write(0, uuid);

    packet
        .getBytes()
        .write(0, (byte) (int) (location.getYaw() * 256.0F / 360.0F))
        .write(1, (byte) (int) (location.getPitch() * 256.0F / 360.0F));

    WrappedDataWatcher dataWatcher =
        player == null
            ? new WrappedDataWatcher()
            : WrappedDataWatcher.getEntityWatcher(player).deepClone();

    packet.getDataWatcherModifier().write(0, dataWatcher);

    return packet;
  }

  @Override
  public Object destroyEntitiesPacket(int... entityIds) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);

    packet.getIntegerArrays().write(0, entityIds);

    return packet;
  }

  static EnumWrappers.PlayerInfoAction convertPlayerInfoAction(
      EnumPlayerInfoAction enumPlayerInfoAction) {
    switch (enumPlayerInfoAction) {
      case ADD_PLAYER:
        return EnumWrappers.PlayerInfoAction.ADD_PLAYER;
      case UPDATE_GAME_MODE:
        return EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE;
      case UPDATE_LATENCY:
        return EnumWrappers.PlayerInfoAction.UPDATE_LATENCY;
      case UPDATE_DISPLAY_NAME:
        return EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME;
      case REMOVE_PLAYER:
      default:
        return EnumWrappers.PlayerInfoAction.REMOVE_PLAYER;
    }
  }

  @Override
  public Object createPlayerInfoPacket(EnumPlayerInfoAction action) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);

    packet.getPlayerInfoAction().write(0, convertPlayerInfoAction(action));

    return packet;
  }

  @Override
  public void setPotionParticles(Player player, boolean enabled) {
    WrappedDataWatcher dataWatcher = WrappedDataWatcher.getEntityWatcher(player);

    if (enabled) {
      Collection<PotionEffect> activePotionEffects = player.getActivePotionEffects();
      for (PotionEffect potionEffect : activePotionEffects) {
        if (!potionEffect.isAmbient()) {
          dataWatcher.setObject(8, false, true);
          dataWatcher.setObject(7, potionEffect.getType().getId(), true);
        }
      }
    }
    dataWatcher.setObject(7, (int) 0, true);
    dataWatcher.setObject(8, true, true);
  }

  @Override
  public RayBlockIntersection getTargetedBLock(Player player) {
    Block targetBlock = player.getTargetBlock(Sets.newHashSet(Material.AIR), 4);

    // this hit location will cause some particles to hide inside the block in adventure mode maps
    // with blockdrops
    Vector hitLocation = targetBlock.getLocation().toVector().add(new Vector(0.5, 0.5, 0.5));

    // BlockFace is unused, default up
    return new RayBlockIntersection(targetBlock, BlockFace.UP, hitLocation);
  }

  @Override
  public boolean playerInfoDataListNotEmpty(Object packet) {
    PacketContainer packetContainer = (PacketContainer) packet;

    StructureModifier<List<PlayerInfoData>> playerInfoDataListsModifier =
        packetContainer.getPlayerInfoDataLists();
    return !playerInfoDataListsModifier.read(0).isEmpty();
  }

  @Override
  public Object playerListPacketData(
      Object packetPlayOutPlayerInfo,
      UUID uuid,
      String name,
      GameMode gamemode,
      int ping,
      @Nullable Skin skin,
      @Nullable String renderedDisplayName) {

    WrappedGameProfile wrappedGameProfile = new WrappedGameProfile(uuid, name);

    if (skin != null) {
      WrappedSignedProperty property =
          WrappedSignedProperty.fromValues("textures", skin.getData(), skin.getSignature());
      wrappedGameProfile.getProperties().put("textures", property);
    }

    EnumWrappers.NativeGameMode nativeGameMode =
        gamemode == null
            ? EnumWrappers.NativeGameMode.CREATIVE
            : EnumWrappers.NativeGameMode.fromBukkit(gamemode);

    WrappedChatComponent wrappedChatComponent =
        renderedDisplayName == null
            ? WrappedChatComponent.fromText("")
            : WrappedChatComponent.fromJson(renderedDisplayName);

    return new PlayerInfoData(wrappedGameProfile, ping, nativeGameMode, wrappedChatComponent);
  }

  @Override
  public void addPlayerInfoToPacket(Object packet, Object playerInfoData) {
    PacketContainer packetContainer = (PacketContainer) packet;

    StructureModifier<List<PlayerInfoData>> playerInfoDataListsModifier =
        packetContainer.getPlayerInfoDataLists();
    List<PlayerInfoData> playerInfoDataList = playerInfoDataListsModifier.read(0);
    playerInfoDataList.add((PlayerInfoData) playerInfoData);
    playerInfoDataListsModifier.write(0, playerInfoDataList);
  }

  @Override
  public Skin getPlayerSkin(Player player) {
    for (WrappedSignedProperty wrappedSignedProperty :
        WrappedGameProfile.fromPlayer(player).getProperties().get("textures")) {
      // return the first texture
      return new Skin(wrappedSignedProperty.getValue(), wrappedSignedProperty.getSignature());
    }

    return null;
  }

  @Override
  public void updateVelocity(Player player) {
    Vector velocity = player.getVelocity();
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_VELOCITY);
    packet
        .getIntegers()
        .write(0, player.getEntityId())
        .write(1, (int) (velocity.getX() * 8000))
        .write(2, (int) (velocity.getY() * 8000))
        .write(3, (int) (velocity.getZ() * 8000));
    sendPacket(player, packet);
  }

  @Override
  public void sendSpawnEntityPacket(
      Player player, int entityId, Location location, Vector velocity) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
    packet
        .getIntegers()
        .write(0, entityId)
        .write(1, (int) (velocity.getX() * 8000))
        .write(2, (int) (velocity.getY() * 8000))
        .write(3, (int) (velocity.getZ() * 8000))
        .write(4, (int) (location.getPitch() * 256.0F / 360.0F))
        .write(5, (int) (location.getYaw() * 256.0F / 360.0F))
        .write(6, 66);

    packet.getUUIDs().write(0, UUID.randomUUID());

    packet
        .getDoubles()
        .write(0, location.getX())
        .write(1, location.getY())
        .write(2, location.getZ());

    sendPacket(player, packet);
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
      Location location = player.getLocation().subtract(0, 1.1, 0);
      Vector velocity = new Vector();

      spawnFakeArmorStand(player, entityId, location, velocity);
    }
  }

  @Override
  public void spawnFakeArmorStand(Player player, int entityId, Location location, Vector velocity) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
    packet
        .getIntegers()
        .write(0, entityId)
        .write(1, 30) // armor stand
        .write(2, (int) (velocity.getX() * 8000))
        .write(3, (int) (velocity.getY() * 8000))
        .write(4, (int) (velocity.getZ() * 8000));
    packet
        .getDoubles()
        .write(0, location.getX())
        .write(1, location.getY())
        .write(2, location.getZ());

    packet
        .getBytes()
        .write(0, (byte) (int) (location.getYaw() * 256.0F / 360.0F))
        .write(1, (byte) (int) (location.getPitch() * 256.0F / 360.0F))
        .write(2, (byte) (int) (location.getPitch() * 256.0F / 360.0F));

    packet.getUUIDs().write(0, UUID.randomUUID());

    WrappedDataWatcher dataWatcher = new WrappedDataWatcher();
    dataWatcher.setObject(
        new WrappedDataWatcher.WrappedDataWatcherObject(
            0, WrappedDataWatcher.Registry.get(Byte.class)),
        (byte) 0x20);
    dataWatcher.setObject(
        new WrappedDataWatcher.WrappedDataWatcherObject(
            4, WrappedDataWatcher.Registry.get(Boolean.class)),
        true);
    dataWatcher.setObject(
        new WrappedDataWatcher.WrappedDataWatcherObject(
            10, WrappedDataWatcher.Registry.get(Byte.class)),
        (byte) 0x8);

    packet.getDataWatcherModifier().write(0, dataWatcher);

    sendPacket(player, packet);
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
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);

    String nameTagVisString = null;
    if (nameTagVisibility != null) {
      switch (nameTagVisibility) {
        case ALWAYS:
          nameTagVisString = "always";
          break;
        case NEVER:
          nameTagVisString = "never";
          break;
        case HIDE_FOR_OTHER_TEAMS:
          nameTagVisString = "hideForOtherTeams";
          break;
        case HIDE_FOR_OWN_TEAM:
          nameTagVisString = "hideForOwnTeam";
          break;
      }
    }

    packet
        .getStrings()
        .write(0, name)
        .write(1, displayName)
        .write(2, prefix)
        .write(3, suffix)
        .write(4, nameTagVisString);

    int flags = 0;
    if (friendlyFire) {
      flags |= 1;
    }
    if (seeFriendlyInvisibles) {
      flags |= 2;
    }

    packet
        .getIntegers()
        .write(0, -1) // color
        .write(1, operation)
        .write(2, flags);

    packet.getSpecificModifier(Collection.class).write(0, players);

    return packet;
  }
}
