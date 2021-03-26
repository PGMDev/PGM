package tc.oc.pgm.util.nms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.server.v1_8_R3.*;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.WorldBorder;
import org.bukkit.*;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_8_R3.entity.*;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.scoreboard.CraftTeam;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.*;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.attribute.AttributeModifier;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.reflect.ReflectionUtils;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.skin.Skins;

public interface NMSHacks {

  AtomicInteger ENTITY_IDS = new AtomicInteger(Integer.MAX_VALUE);

  static EntityTrackerEntry getTrackerEntry(net.minecraft.server.v1_8_R3.Entity nms) {
    return ((WorldServer) nms.getWorld()).getTracker().trackedEntities.get(nms.getId());
  }

  static EntityTrackerEntry getTrackerEntry(Entity entity) {
    return getTrackerEntry(((CraftEntity) entity).getHandle());
  }

  static void sendPacket(Object packet) {
    for (Player pl : Bukkit.getOnlinePlayers()) {
      sendPacket(pl, packet);
    }
  }

  static void sendPacket(Player bukkitPlayer, Object packet) {
    if (bukkitPlayer.isOnline()) {
      EntityPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
      nmsPlayer.playerConnection.sendPacket((Packet) packet);
    }
  }

  @SuppressWarnings("unchecked")
  static void sendPacketToViewers(Entity entity, Object packet) {
    EntityTrackerEntry entry = getTrackerEntry(entity);
    for (EntityPlayer viewer : ((Set<EntityPlayer>) entry.trackedPlayers)) {
      viewer.playerConnection.sendPacket((Packet) packet);
    }
  }

  Constructor<PacketPlayOutPlayerInfo.PlayerInfoData> playerInfoDataConstructor =
      getPlayerInfoDataConstructor();

  static Constructor<PacketPlayOutPlayerInfo.PlayerInfoData> getPlayerInfoDataConstructor() {
    try {
      Constructor<PacketPlayOutPlayerInfo.PlayerInfoData> constructor =
          PacketPlayOutPlayerInfo.PlayerInfoData.class.getConstructor(
              PacketPlayOutPlayerInfo.class,
              GameProfile.class,
              int.class,
              WorldSettings.EnumGamemode.class,
              IChatBaseComponent.class);

      constructor.setAccessible(true);
      return constructor;
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  static PacketPlayOutPlayerInfo.PlayerInfoData playerListPacketData(
      PacketPlayOutPlayerInfo packet,
      UUID uuid,
      String name,
      GameMode gamemode,
      int ping,
      @Nullable Skin skin,
      @Nullable BaseComponent... displayName) {
    GameProfile profile = new GameProfile(uuid, name);
    if (skin != null) {
      for (Map.Entry<String, Collection<Property>> entry :
          Skins.toProperties(skin).asMap().entrySet()) {
        profile.getProperties().putAll(entry.getKey(), entry.getValue());
      }
    }

    if (BukkitUtils.isSportPaper()) {
      PacketPlayOutPlayerInfo.PlayerInfoData data =
          packet.constructData(
              profile,
              ping,
              gamemode == null ? null : WorldSettings.EnumGamemode.getById(gamemode.getValue()),
              null); // ELECTROID
      data.displayName = displayName == null || displayName.length == 0 ? null : displayName;
      return data;
    } else {
      try {
        WorldSettings.EnumGamemode enumGamemode =
            gamemode == null ? null : WorldSettings.EnumGamemode.getById(gamemode.getValue());
        IChatBaseComponent iChatBaseComponent =
            displayName == null || displayName.length == 0
                ? null
                : IChatBaseComponent.ChatSerializer.a(
                    net.md_5.bungee.chat.ComponentSerializer.toString(displayName));

        return playerInfoDataConstructor.newInstance(
            packet, profile, ping, enumGamemode, iChatBaseComponent);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
  }

  Field playerInfoActionField = ReflectionUtils.getField(PacketPlayOutPlayerInfo.class, "a");

  static PacketPlayOutPlayerInfo createPlayerInfoPacket(
      PacketPlayOutPlayerInfo.EnumPlayerInfoAction action) {
    PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
    if (BukkitUtils.isSportPaper()) {
      packet.a = action;
    } else {
      ReflectionUtils.setField(packet, action, playerInfoActionField);
    }
    return packet;
  }

  static PacketPlayOutPlayerInfo.PlayerInfoData playerListPacketData(
      PacketPlayOutPlayerInfo packet, UUID uuid, BaseComponent... displayName) {
    return playerListPacketData(
        packet, uuid, "|" + uuid.toString().substring(0, 15), null, 0, null, displayName);
  }

  static PacketPlayOutPlayerInfo.PlayerInfoData playerListPacketData(
      PacketPlayOutPlayerInfo packet, UUID uuid) {
    return playerListPacketData(packet, uuid, null, null, 0, null);
  }

  static PacketPlayOutPlayerInfo.PlayerInfoData playerListPacketData(
      PacketPlayOutPlayerInfo packet, UUID uuid, int ping) {
    return playerListPacketData(packet, uuid, uuid.toString().substring(0, 16), null, ping, null);
  }

  /**
   * Removes all players from the tab for the viewer and re-adds them
   *
   * @param viewer The viewer to send the packets to
   */
  static void removeAndAddAllTabPlayers(Player viewer) {
    List<EntityPlayer> players = new ArrayList<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (viewer.canSee(player) || player == viewer)
        players.add(((CraftPlayer) player).getHandle());
    }

    sendPacket(
        viewer,
        new PacketPlayOutPlayerInfo(
            PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, players));
    sendPacket(
        viewer,
        new PacketPlayOutPlayerInfo(
            PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, players));
  }

  enum TeamPacketFields {
    a,
    b,
    c,
    d,
    e,
    g,
    h,
    i;

    Field field;

    TeamPacketFields() {
      field = ReflectionUtils.getField(PacketPlayOutScoreboardTeam.class, name());
    }

    public Field getField() {
      return field;
    }
  }

  static Packet teamPacket(
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

    if (BukkitUtils.isSportPaper()) {
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
    } else {
      ReflectionUtils.setField(packet, name, TeamPacketFields.a.getField());
      ReflectionUtils.setField(packet, displayName, TeamPacketFields.b.getField());
      ReflectionUtils.setField(packet, prefix, TeamPacketFields.c.getField());
      ReflectionUtils.setField(packet, suffix, TeamPacketFields.d.getField());

      String e = null;
      if (nameTagVisibility != null) {
        switch (nameTagVisibility) {
          case ALWAYS:
            e = "always";
            break;
          case NEVER:
            e = "never";
            break;
          case HIDE_FOR_OTHER_TEAMS:
            e = "hideForOtherTeams";
            break;
          case HIDE_FOR_OWN_TEAM:
            e = "hideForOwnTeam";
            break;
        }
      }

      ReflectionUtils.setField(packet, e, TeamPacketFields.e.getField());
      ReflectionUtils.setField(packet, players, TeamPacketFields.g.getField());
      ReflectionUtils.setField(packet, operation, TeamPacketFields.h.getField());

      int i = (int) ReflectionUtils.readField(packet, TeamPacketFields.i.getField());
      if (friendlyFire) {
        i |= 1;
      }
      if (seeFriendlyInvisibles) {
        i |= 2;
      }

      ReflectionUtils.setField(packet, i, TeamPacketFields.i.getField());
    }
    return packet;
  }

  static Packet teamCreatePacket(
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

  static Packet teamRemovePacket(String name) {
    return teamPacket(1, name, null, null, null, false, false, null, Lists.<String>newArrayList());
  }

  static Packet teamUpdatePacket(
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

  static Packet teamJoinPacket(String name, Collection<String> players) {
    return teamPacket(3, name, null, null, null, false, false, null, players);
  }

  static Packet teamLeavePacket(String name, Collection<String> players) {
    return teamPacket(4, name, null, null, null, false, false, null, players);
  }

  static int allocateEntityId() {
    return ENTITY_IDS.decrementAndGet();
  }

  static void sendLegacyWearing(Player player, int slot, ItemStack item) {
    Packet<?> packet =
        new PacketPlayOutEntityEquipment(
            player.getEntityId(), slot, CraftItemStack.asNMSCopy(item));
    EntityTrackerEntry entry = getTrackerEntry(player);
    for (EntityPlayer viewer : entry.trackedPlayers) {
      if (ViaUtils.getProtocolVersion(viewer.getBukkitEntity()) <= ViaUtils.VERSION_1_7)
        viewer.playerConnection.sendPacket(packet);
    }
  }

  class EntityMetadata {
    public final DataWatcher dataWatcher;

    public EntityMetadata(DataWatcher watcher) {
      dataWatcher = watcher;
    }

    static EntityMetadata clone(DataWatcher original) {
      List<DataWatcher.WatchableObject> values = original.c();
      DataWatcher copy = new DataWatcher(null);
      for (DataWatcher.WatchableObject value : values) {
        copy.a(value.a(), value.b());
      }
      return new EntityMetadata(copy);
    }

    static EntityMetadata clone(Entity entity) {
      return clone(((CraftEntity) entity).getHandle().getDataWatcher());
    }

    @Override
    public EntityMetadata clone() {
      return clone(this.dataWatcher);
    }
  }

  static Packet destroyEntitiesPacket(int... entityIds) {
    return new PacketPlayOutEntityDestroy(entityIds);
  }

  static void destroyEntities(Player player, int... entityIds) {
    sendPacket(player, destroyEntitiesPacket(entityIds));
  }

  static Packet spawnPlayerPacket(int entityId, UUID uuid, Location location, Player player) {
    return spawnPlayerPacket(entityId, uuid, location, null, EntityMetadata.clone(player));
  }

  enum NamedEntitySpawnFields {
    a,
    b,
    c,
    d,
    e,
    f,
    g,
    h,
    i,
    j;

    Field field;

    NamedEntitySpawnFields() {
      field = ReflectionUtils.getField(PacketPlayOutNamedEntitySpawn.class, name());
    }

    public Field getField() {
      return field;
    }
  }

  static Packet spawnPlayerPacket(
      int entityId,
      UUID uuid,
      Location location,
      org.bukkit.inventory.ItemStack heldItem,
      EntityMetadata metadata) {
    if (BukkitUtils.isSportPaper()) {
      return new PacketPlayOutNamedEntitySpawn(
          entityId,
          uuid,
          location.getX(),
          location.getY(),
          location.getZ(),
          (byte) location.getYaw(),
          (byte) location.getPitch(),
          CraftItemStack.asNMSCopy(heldItem),
          metadata.dataWatcher);
    } else {
      PacketPlayOutNamedEntitySpawn packet = new PacketPlayOutNamedEntitySpawn();

      ReflectionUtils.setField(packet, entityId, NamedEntitySpawnFields.a.getField());
      ReflectionUtils.setField(packet, uuid, NamedEntitySpawnFields.b.getField());
      ReflectionUtils.setField(
          packet, MathHelper.floor(location.getX() * 32.0D), NamedEntitySpawnFields.c.getField());
      ReflectionUtils.setField(
          packet, MathHelper.floor(location.getY() * 32.0D), NamedEntitySpawnFields.d.getField());
      ReflectionUtils.setField(
          packet, MathHelper.floor(location.getZ() * 32.0D), NamedEntitySpawnFields.e.getField());
      ReflectionUtils.setField(
          packet,
          (byte) ((int) (((byte) location.getYaw()) * 256.0F / 360.0F)),
          NamedEntitySpawnFields.f.getField());
      ReflectionUtils.setField(
          packet,
          (byte) ((int) (((byte) location.getPitch()) * 256.0F / 360.0F)),
          NamedEntitySpawnFields.g.getField());
      ReflectionUtils.setField(
          packet,
          heldItem == null ? 0 : Item.getId(CraftItemStack.asNMSCopy(heldItem).getItem()),
          NamedEntitySpawnFields.h.getField());
      ReflectionUtils.setField(packet, metadata.dataWatcher, NamedEntitySpawnFields.i.getField());
      ReflectionUtils.setField(
          packet, metadata.dataWatcher.b(), NamedEntitySpawnFields.j.getField());

      return packet;
    }
  }

  static void spawnLivingEntity(
      Player player, EntityType type, int entityId, Location location, EntityMetadata metadata) {
    sendPacket(player, spawnLivingEntityPacket(type, entityId, location, metadata));
  }

  enum LivingEntitySpawnFields {
    a,
    b,
    c,
    d,
    e,
    i,
    j,
    k,
    l;

    Field field;

    LivingEntitySpawnFields() {
      field = ReflectionUtils.getField(PacketPlayOutSpawnEntityLiving.class, name());
    }

    public Field getField() {
      return field;
    }
  }

  @SuppressWarnings("deprecation")
  static Packet spawnLivingEntityPacket(
      EntityType type, int entityId, Location location, EntityMetadata metadata) {
    if (BukkitUtils.isSportPaper()) {
      return new PacketPlayOutSpawnEntityLiving(
          entityId,
          (byte) type.getTypeId(),
          location.getX(),
          location.getY(),
          location.getZ(),
          location.getYaw(),
          location.getPitch(),
          location.getPitch(),
          0,
          0,
          0,
          metadata.dataWatcher);
    } else {
      PacketPlayOutSpawnEntityLiving packet = new PacketPlayOutSpawnEntityLiving();

      ReflectionUtils.setField(packet, entityId, LivingEntitySpawnFields.a.getField());
      ReflectionUtils.setField(
          packet, (byte) type.getTypeId(), LivingEntitySpawnFields.b.getField());
      ReflectionUtils.setField(
          packet, MathHelper.floor(location.getX() * 32.0D), LivingEntitySpawnFields.c.getField());
      ReflectionUtils.setField(
          packet, MathHelper.floor(location.getY() * 32.0D), LivingEntitySpawnFields.d.getField());
      ReflectionUtils.setField(
          packet, MathHelper.floor(location.getZ() * 32.0D), LivingEntitySpawnFields.e.getField());
      ReflectionUtils.setField(
          packet,
          (byte) ((int) (((byte) location.getYaw()) * 256.0F / 360.0F)),
          LivingEntitySpawnFields.i.getField());
      ReflectionUtils.setField(
          packet,
          (byte) ((int) (((byte) location.getPitch()) * 256.0F / 360.0F)),
          LivingEntitySpawnFields.j.getField());
      ReflectionUtils.setField(
          packet,
          (byte) ((int) (((byte) location.getPitch()) * 256.0F / 360.0F)),
          LivingEntitySpawnFields.k.getField());
      ReflectionUtils.setField(packet, metadata.dataWatcher, LivingEntitySpawnFields.l.getField());

      return packet;
    }
  }

  static void spawnEntity(Player player, int type, int entityId, Location location) {
    sendPacket(player, spawnEntityPacket(type, entityId, location));
  }

  enum EntitySpawnFields {
    a,
    b,
    c,
    d,
    h,
    i,
    j;

    Field field;

    EntitySpawnFields() {
      field = ReflectionUtils.getField(PacketPlayOutSpawnEntity.class, name());
    }

    public Field getField() {
      return field;
    }
  }

  static Packet spawnEntityPacket(int type, int entityId, Location location) {
    if (BukkitUtils.isSportPaper()) {
      return new PacketPlayOutSpawnEntity(
          entityId,
          location.getX(),
          location.getY(),
          location.getZ(),
          0,
          0,
          0,
          (int) location.getPitch(),
          (int) location.getYaw(),
          type,
          0);
    } else {
      PacketPlayOutSpawnEntity packet = new PacketPlayOutSpawnEntity();

      ReflectionUtils.setField(packet, entityId, EntitySpawnFields.a.getField());
      ReflectionUtils.setField(
          packet, MathHelper.floor(location.getX() * 32.0D), EntitySpawnFields.b.getField());
      ReflectionUtils.setField(
          packet, MathHelper.floor(location.getY() * 32.0D), EntitySpawnFields.c.getField());
      ReflectionUtils.setField(
          packet, MathHelper.floor(location.getZ() * 32.0D), EntitySpawnFields.d.getField());
      ReflectionUtils.setField(
          packet,
          (byte) ((int) (((byte) location.getYaw()) * 256.0F / 360.0F)),
          EntitySpawnFields.h.getField());
      ReflectionUtils.setField(
          packet,
          (byte) ((int) (((byte) location.getPitch()) * 256.0F / 360.0F)),
          EntitySpawnFields.i.getField());
      ReflectionUtils.setField(packet, type, EntitySpawnFields.j.getField());

      return packet;
    }
  }

  static void spawnFreezeEntity(Player player, int entityId, boolean legacy) {
    if (legacy) {
      Location location = player.getLocation().add(0, 0.286, 0);
      if (location.getY() < -64) {
        location.setY(-64);
        player.teleport(location);
      }

      NMSHacks.spawnEntity(player, 66, entityId, location);
    } else {
      Location loc = player.getLocation().subtract(0, 1.1, 0);

      NMSHacks.EntityMetadata metadata = NMSHacks.createEntityMetadata();
      NMSHacks.setEntityMetadata(metadata, false, false, false, false, true, (short) 0);
      NMSHacks.setArmorStandFlags(metadata, false, false, false, false);
      NMSHacks.spawnLivingEntity(player, EntityType.ARMOR_STAND, entityId, loc, metadata);
    }
  }

  enum EntityAttachFields {
    a,
    b,
    c;

    Field field;

    EntityAttachFields() {
      field = ReflectionUtils.getField(PacketPlayOutAttachEntity.class, name());
    }

    public Field getField() {
      return field;
    }
  }

  static void entityAttach(Player player, int entityID, int vehicleID, boolean leash) {
    if (BukkitUtils.isSportPaper()) {
      sendPacket(player, new PacketPlayOutAttachEntity(entityID, vehicleID, leash));
    } else {
      PacketPlayOutAttachEntity packet = new PacketPlayOutAttachEntity();

      ReflectionUtils.setField(packet, (byte) (leash ? 1 : 0), EntityAttachFields.a.getField());
      ReflectionUtils.setField(packet, entityID, EntityAttachFields.b.getField());
      ReflectionUtils.setField(packet, vehicleID, EntityAttachFields.c.getField());

      sendPacket(player, packet);
    }
  }

  static Packet teleportEntityPacket(int entityId, Location location) {
    return new PacketPlayOutEntityTeleport(
        entityId, // Entity ID
        (int) (location.getX() * 32), // World X * 32
        (int) (location.getY() * 32), // World Y * 32
        (int) (location.getZ() * 32), // World Z * 32
        (byte) (location.getYaw() * 256 / 360), // Yaw
        (byte) (location.getPitch() * 256 / 360), // Pitch
        true); // On Ground + Height Correction
  }

  static Packet entityMetadataPacket(int entityId, Entity entity, boolean complete) {
    return new PacketPlayOutEntityMetadata(
        entityId,
        ((CraftEntity) entity).getHandle().getDataWatcher(),
        complete); // true = all values, false = only dirty values
  }

  static EntityMetadata createEntityMetadata() {
    return new EntityMetadata(new DataWatcher(null));
  }

  static void setEntityMetadata(EntityMetadata metadata, byte flags, short air) {
    DataWatcher dataWatcher = metadata.dataWatcher;
    dataWatcher.a(0, (byte) flags);
    dataWatcher.a(1, (short) air);
  }

  static void setEntityMetadata(
      EntityMetadata metadata,
      boolean onFire,
      boolean crouched,
      boolean sprinting,
      boolean eatingOrBlocking,
      boolean invisible,
      short air) {
    int flags = 0;
    if (onFire) flags |= 0x01;
    if (crouched) flags |= 0x02;
    if (sprinting) flags |= 0x08;
    if (eatingOrBlocking) flags |= 0x10;
    if (invisible) flags |= 0x20;
    setEntityMetadata(metadata, (byte) flags, air);
  }

  static void setArmorStandFlags(
      EntityMetadata metadata, boolean small, boolean gravity, boolean arms, boolean baseplate) {
    int flags = 0;
    if (small) flags |= 0x01;
    if (gravity) flags |= 0x02;
    if (arms) flags |= 0x04;
    if (baseplate) flags |= 0x08;
    metadata.dataWatcher.a(10, (byte) flags);
  }

  Method enablePotionParticlesMethod = ReflectionUtils.getMethod(EntityLiving.class, "B");
  Method disablePotionParticlesMethod = ReflectionUtils.getMethod(EntityLiving.class, "bj");

  static void setPotionParticles(Player player, boolean enabled) {
    if (BukkitUtils.isSportPaper()) {
      player.setPotionParticles(enabled);
    } else {
      CraftPlayer craftPlayer = (CraftPlayer) player;
      EntityPlayer handle = craftPlayer.getHandle();

      if (enabled) {
        ReflectionUtils.callMethod(enablePotionParticlesMethod, handle);
      } else {
        ReflectionUtils.callMethod(disablePotionParticlesMethod, handle);
      }
    }
  }

  static void clearArrowsInPlayer(Player player) {
    ((CraftPlayer) player).getHandle().o(0);
  }

  /**
   * Test if the given tool is capable of "efficiently" mining the given block.
   *
   * <p>Derived from CraftBlock.itemCausesDrops()
   */
  static boolean canMineBlock(MaterialData blockMaterial, org.bukkit.inventory.ItemStack tool) {
    if (!blockMaterial.getItemType().isBlock()) {
      throw new IllegalArgumentException("Material '" + blockMaterial + "' is not a block");
    }

    net.minecraft.server.v1_8_R3.Block nmsBlock =
        CraftMagicNumbers.getBlock(blockMaterial.getItemType());
    net.minecraft.server.v1_8_R3.Item nmsTool =
        tool == null ? null : CraftMagicNumbers.getItem(tool.getType());

    return nmsBlock != null
        && (nmsBlock.getMaterial().isAlwaysDestroyable()
            || (nmsTool != null && nmsTool.canDestroySpecialBlock(nmsBlock)));
  }

  static long getMonotonicTime(World world) {
    return ((CraftWorld) world).getHandle().getTime();
  }

  static void sendMessage(Player player, BaseComponent[] message, int position) {
    PacketPlayOutChat packet = new PacketPlayOutChat(null, (byte) position);
    packet.components = message;
    sendPacket(player, packet);
  }

  static void showBorderWarning(Player player, boolean show) {
    WorldBorder border = new WorldBorder();
    border.setWarningDistance(show ? Integer.MAX_VALUE : 0);
    sendPacket(
        player,
        new PacketPlayOutWorldBorder(
            border, PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_WARNING_BLOCKS));
  }

  Field entityMetadataWatchableField =
      ReflectionUtils.getField(PacketPlayOutEntityMetadata.class, "b");

  static void playDeathAnimation(Player player) {
    EntityPlayer handle = ((CraftPlayer) player).getHandle();
    PacketPlayOutEntityMetadata metadata =
        new PacketPlayOutEntityMetadata(handle.getId(), handle.getDataWatcher(), false);

    // Add/replace health to zero
    boolean replaced = false;
    DataWatcher.WatchableObject zeroHealth =
        new DataWatcher.WatchableObject(3, 6, 0f); // type 3 (float), index 6 (health)

    List<DataWatcher.WatchableObject> b =
        (List<DataWatcher.WatchableObject>)
            ReflectionUtils.readField(metadata, entityMetadataWatchableField);
    if (b != null) {
      for (int i = 0; i < b.size(); i++) {
        DataWatcher.WatchableObject wo = b.get(i);
        if (wo.a() == 6) {
          b.set(i, zeroHealth);
          replaced = true;
        }
      }
    }

    if (!replaced) {
      if (b != null) b.add(zeroHealth);
      else
        ReflectionUtils.setField(
            metadata, Collections.singletonList(zeroHealth), entityMetadataWatchableField);
    }

    Location location = player.getLocation();
    PacketPlayOutBed useBed =
        new PacketPlayOutBed(
            ((CraftPlayer) player).getHandle(),
            new BlockPosition(location.getX(), location.getY(), location.getZ()));

    Packet<?> teleport = teleportEntityPacket(player.getEntityId(), location);

    sendPacketToViewers(player, metadata);
    sendPacketToViewers(player, useBed);
    sendPacketToViewers(player, teleport);
  }

  static org.bukkit.enchantments.Enchantment getEnchantment(String key) {
    Enchantment enchantment = Enchantment.getByName(key);
    return enchantment == null ? null : org.bukkit.enchantments.Enchantment.getById(enchantment.id);
  }

  static PotionEffectType getPotionEffectType(String key) {
    MobEffectList nms = MobEffectList.b(key);
    return nms == null ? null : PotionEffectType.getById(nms.id);
  }

  static Set<MaterialData> getBlockStates(Material material) {
    ImmutableSet.Builder<MaterialData> materials = ImmutableSet.builder();
    Block nmsBlock = CraftMagicNumbers.getBlock(material);
    List<IBlockData> states =
        ReflectionUtils.readField(BlockStateList.class, nmsBlock.P(), List.class, "e");
    if (states != null) {
      for (IBlockData state : states) {
        int data = nmsBlock.toLegacyData(state);
        materials.add(material.getNewData((byte) data));
      }
    }
    return materials.build();
  }

  static void setAbsorption(LivingEntity entity, double health) {
    ((CraftLivingEntity) entity).getHandle().setAbsorptionHearts((float) health);
  }

  static double getAbsorption(LivingEntity entity) {
    return ((CraftLivingEntity) entity).getHandle().getAbsorptionHearts();
  }

  static int getProtocolVersion(Player player) {
    if (BukkitUtils.isSportPaper()) {
      return ((CraftPlayer) player).getHandle().playerConnection.networkManager.protocolVersion;
    } else {
      return ViaUtils.VERSION_1_8; // Assume 1.8 if not sportpaper.
    }
  }

  static int getPing(Player player) {
    return ((CraftPlayer) player).getHandle().ping;
  }

  static Packet entityEquipmentPacket(
      int entityId, int slot, org.bukkit.inventory.ItemStack armor) {
    return new PacketPlayOutEntityEquipment(entityId, slot, CraftItemStack.asNMSCopy(armor));
  }

  static Skin getPlayerSkin(Player player) {
    CraftPlayer craftPlayer = (CraftPlayer) player;
    return Skins.fromProperties(craftPlayer.getProfile().getProperties());
  }

  Field skullProfileField =
      ReflectionUtils.getField(
          "org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaSkull", "profile");

  static void setSkullMetaOwner(SkullMeta meta, String name, UUID uuid, Skin skin) {
    GameProfile gameProfile = new GameProfile(uuid, name);
    Skins.setProperties(skin, gameProfile.getProperties());
    ReflectionUtils.setField(meta, gameProfile, skullProfileField);
  }

  static Set<org.bukkit.block.Block> getBlocks(Chunk bukkitChunk, Material material) {
    CraftChunk craftChunk = (CraftChunk) bukkitChunk;
    Set<org.bukkit.block.Block> blocks = new HashSet<>();

    net.minecraft.server.v1_8_R3.Block nmsBlock = CraftMagicNumbers.getBlock(material);
    net.minecraft.server.v1_8_R3.Chunk chunk = craftChunk.getHandle();

    for (ChunkSection section : chunk.getSections()) {
      if (section == null || section.a()) continue; // ChunkSection.a() -> true if section is empty

      char[] blockIds = section.getIdArray();
      for (int i = 0; i < blockIds.length; i++) {
        // This does a lookup in the block registry, but does not create any objects, so should be
        // pretty efficient
        IBlockData blockData = (IBlockData) net.minecraft.server.v1_8_R3.Block.d.a(blockIds[i]);
        if (blockData != null && blockData.getBlock() == nmsBlock) {
          blocks.add(
              bukkitChunk.getBlock(i & 0xf, section.getYPosition() | (i >> 8), (i >> 4) & 0xf));
        }
      }
    }

    return blocks;
  }

  static WorldCreator detectWorld(String worldName) {
    IDataManager sdm =
        new ServerNBTManager(Bukkit.getServer().getWorldContainer(), worldName, true);
    WorldData worldData = sdm.getWorldData();
    if (worldData == null) return null;

    return new WorldCreator(worldName)
        .generateStructures(worldData.shouldGenerateMapFeatures())
        .generatorSettings(worldData.getGeneratorOptions())
        .seed(worldData.getSeed())
        .type(org.bukkit.WorldType.getByName(worldData.getType().name()));
  }

  static RayBlockIntersection getTargetedBLock(Player player) {
    Location start = player.getEyeLocation();
    World world = player.getWorld();
    Vector startVector = start.toVector();
    Vector end =
        start
            .toVector()
            .add(
                start.getDirection().multiply(player.getGameMode() == GameMode.CREATIVE ? 6 : 4.5));
    MovingObjectPosition hit =
        ((CraftWorld) world)
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

  String CAN_DESTROY = "CanDestroy";
  String CAN_PLACE_ON = "CanPlaceOn";

  static void setCanDestroy(ItemMeta itemMeta, Set<Material> materials) {
    if (BukkitUtils.isSportPaper()) {
      // Since this is handled by SportPaper this can't be done through unhandled tags
      itemMeta.setCanDestroy(materials);
    }
    setMaterialList(itemMeta, materials, CAN_DESTROY);
  }

  static Collection<Material> getCanDestroy(ItemMeta itemMeta) {
    if (BukkitUtils.isSportPaper()) {
      // Since this is handled by SportPaper this can't be done through unhandled tags
      return itemMeta.getCanDestroy();
    }
    return getMaterialCollection(itemMeta, CAN_DESTROY);
  }

  static void setCanPlaceOn(ItemMeta itemMeta, Set<Material> materials) {
    if (BukkitUtils.isSportPaper()) {
      // Since this is handled by SportPaper this can't be done through unhandled tags
      itemMeta.setCanPlaceOn(materials);
    }
    setMaterialList(itemMeta, materials, CAN_PLACE_ON);
  }

  static Collection<Material> getCanPlaceOn(ItemMeta itemMeta) {
    if (BukkitUtils.isSportPaper()) {
      // Since this is handled by SportPaper this can't be done through unhandled tags
      return itemMeta.getCanPlaceOn();
    }
    return getMaterialCollection(itemMeta, CAN_PLACE_ON);
  }

  Field unhandledTagsField =
      ReflectionUtils.getField(
          "org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaItem", "unhandledTags");

  static Map<String, NBTBase> getUnhandledTags(ItemMeta meta) {
    return (Map<String, NBTBase>) ReflectionUtils.readField(meta, unhandledTagsField);
  }

  static void setMaterialList(ItemMeta itemMeta, Set<Material> materials, String canPlaceOn) {
    Map<String, NBTBase> unhandledTags = getUnhandledTags(itemMeta);
    NBTTagList canDestroyList =
        unhandledTags.containsKey(canPlaceOn)
            ? (NBTTagList) unhandledTags.get(canPlaceOn)
            : new NBTTagList();
    for (Material material : materials) {
      Block block = Block.getById(material.getId());
      if (block != null) {
        canDestroyList.add(new NBTTagString(Block.REGISTRY.c(block).toString()));
      }
    }
    if (!canDestroyList.isEmpty()) unhandledTags.put(canPlaceOn, canDestroyList);
  }

  Field nbtListField = ReflectionUtils.getField(NBTTagList.class, "list");

  static Set<Material> getMaterialCollection(ItemMeta itemMeta, String key) {
    Map<String, NBTBase> unhandledTags = getUnhandledTags(itemMeta);
    if (!unhandledTags.containsKey(key)) return new HashSet<>();
    Set<Material> materialSet = new HashSet<>();
    NBTTagList canDestroyList = (NBTTagList) unhandledTags.get(key);

    for (NBTBase item : (List<NBTBase>) ReflectionUtils.readField(canDestroyList, nbtListField)) {
      NBTTagString nbtTagString = (NBTTagString) item;
      String blockString = nbtTagString.a_();
      materialSet.add(Material.getMaterial(Block.getId(Block.getByName(blockString))));
    }

    return materialSet;
  }

  static void applyAttributeModifiers(
      SetMultimap<String, AttributeModifier> attributeModifiers, ItemMeta meta) {
    NBTTagList list = new NBTTagList();
    for (Map.Entry<String, AttributeModifier> entry : attributeModifiers.entries()) {
      AttributeModifier modifier = entry.getValue();
      NBTTagCompound tag = new NBTTagCompound();
      tag.setString("Name", modifier.getName());
      tag.setDouble("Amount", modifier.getAmount());
      tag.setInt("Operation", modifier.getOperation().ordinal());
      tag.setLong("UUIDMost", modifier.getUniqueId().getMostSignificantBits());
      tag.setLong("UUIDLeast", modifier.getUniqueId().getLeastSignificantBits());
      tag.setString("AttributeName", entry.getKey());
      list.add(tag);
    }

    Map<String, NBTBase> unhandledTags = getUnhandledTags(meta);
    unhandledTags.put("AttributeModifiers", list);
  }

  static SetMultimap<String, AttributeModifier> getAttributeModifiers(ItemMeta meta) {
    Map<String, NBTBase> unhandledTags = getUnhandledTags(meta);
    if (unhandledTags.containsKey("AttributeModifiers")) {
      final SetMultimap<String, AttributeModifier> attributeModifiers = HashMultimap.create();
      final NBTTagList modTags = (NBTTagList) unhandledTags.get("AttributeModifiers");
      for (int i = 0; i < modTags.size(); i++) {
        final NBTTagCompound modTag = modTags.get(i);
        attributeModifiers.put(
            modTag.getString("AttributeName"),
            new AttributeModifier(
                new UUID(modTag.getLong("UUIDMost"), modTag.getLong("UUIDLeast")),
                modTag.getString("Name"),
                modTag.getDouble("Amount"),
                AttributeModifier.Operation.fromOpcode(modTag.getInt("Operation"))));
      }
      return attributeModifiers;
    } else {
      return HashMultimap.create();
    }
  }

  interface FakeEntity {
    int entityId();

    Entity entity();

    void spawn(Player viewer, Location location, org.bukkit.util.Vector velocity);

    default void spawn(Player viewer, Location location) {
      spawn(viewer, location, new org.bukkit.util.Vector(0, 0, 0));
    }

    default void destroy(Player viewer) {
      sendPacket(viewer, destroyEntitiesPacket(entityId()));
    }

    default void teleport(Player viewer, Location location) {
      sendPacket(viewer, teleportEntityPacket(entityId(), location));
    }

    default void ride(Player viewer, Entity rider) {
      entityAttach(viewer, rider.getEntityId(), entityId(), false);
    }

    default void mount(Player viewer, Entity vehicle) {
      entityAttach(viewer, entityId(), vehicle.getEntityId(), false);
    }

    default void wear(Player viewer, int slot, org.bukkit.inventory.ItemStack item) {
      sendPacket(viewer, entityEquipmentPacket(entityId(), slot, item));
    }
  }

  abstract class FakeEntityImpl<T extends net.minecraft.server.v1_8_R3.Entity>
      implements FakeEntity {
    protected final T entity;

    protected FakeEntityImpl(T entity) {
      this.entity = entity;
    }

    @Override
    public Entity entity() {
      return entity.getBukkitEntity();
    }

    @Override
    public void spawn(Player viewer, Location location, Vector velocity) {
      entity.setPositionRotation(
          location.getX(),
          location.getY(),
          location.getZ(),
          location.getYaw(),
          location.getPitch());
      entity.motX = velocity.getX();
      entity.motY = velocity.getY();
      entity.motZ = velocity.getZ();
      sendPacket(viewer, spawnPacket());
    }

    abstract Packet<?> spawnPacket();

    @Override
    public int entityId() {
      return entity.getId();
    }
  }

  class FakeLivingEntity<T extends EntityLiving> extends FakeEntityImpl<T> {

    protected FakeLivingEntity(T entity) {
      super(entity);
    }

    protected Packet<?> spawnPacket() {
      return new PacketPlayOutSpawnEntityLiving(entity);
    }
  }

  class FakeArmorStand extends FakeLivingEntity<EntityArmorStand> {

    public FakeArmorStand(World world) {
      super(new EntityArmorStand(((CraftWorld) world).getHandle()));

      entity.setInvisible(true);
      NBTTagCompound tag = entity.getNBTTag();
      if (tag == null) {
        tag = new NBTTagCompound();
      }
      entity.c(tag);
      tag.setBoolean("Silent", true);
      tag.setBoolean("Invulnerable", true);
      tag.setBoolean("NoGravity", true);
      tag.setBoolean("NoAI", true);
      entity.f(tag);
    }
  }

  class FakeWitherSkull extends FakeEntityImpl<EntityWitherSkull> {
    public FakeWitherSkull(World world) {
      super(new EntityWitherSkull(((CraftWorld) world).getHandle()));
    }

    protected Packet<?> spawnPacket() {
      return new PacketPlayOutSpawnEntity(entity, 66);
    }

    @Override
    public void wear(Player viewer, int slot, ItemStack item) {}
  }
}
