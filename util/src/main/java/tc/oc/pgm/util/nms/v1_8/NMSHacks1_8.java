package tc.oc.pgm.util.nms.v1_8;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.BlockStateList;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.EntityArrow;
import net.minecraft.server.v1_8_R3.EntityFireball;
import net.minecraft.server.v1_8_R3.EntityFireworks;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityTrackerEntry;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IDataManager;
import net.minecraft.server.v1_8_R3.MathHelper;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.MobEffectList;
import net.minecraft.server.v1_8_R3.MovingObjectPosition;
import net.minecraft.server.v1_8_R3.NBTBase;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.NBTTagString;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutAttachEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutBed;
import net.minecraft.server.v1_8_R3.PacketPlayOutCollect;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldBorder;
import net.minecraft.server.v1_8_R3.ServerNBTManager;
import net.minecraft.server.v1_8_R3.Vec3D;
import net.minecraft.server.v1_8_R3.WorldBorder;
import net.minecraft.server.v1_8_R3.WorldData;
import net.minecraft.server.v1_8_R3.WorldServer;
import net.minecraft.server.v1_8_R3.WorldSettings;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFireball;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFirework;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftItem;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.attribute.AttributeMap;
import tc.oc.pgm.util.attribute.AttributeModifier;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.nms.EnumPlayerInfoAction;
import tc.oc.pgm.util.nms.NMSHacksNoOp;
import tc.oc.pgm.util.nms.attribute.AttributeMap1_8;
import tc.oc.pgm.util.nms.entity.fake.FakeEntity;
import tc.oc.pgm.util.nms.entity.fake.armorstand.FakeArmorStand1_8;
import tc.oc.pgm.util.nms.entity.fake.wither.FakeWitherSkull1_8;
import tc.oc.pgm.util.nms.entity.potion.EntityPotion;
import tc.oc.pgm.util.nms.entity.potion.EntityPotion1_8;
import tc.oc.pgm.util.reflect.MinecraftReflectionUtils;
import tc.oc.pgm.util.reflect.ReflectionUtils;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.skin.Skins;

public class NMSHacks1_8 extends NMSHacksNoOp {

  public NMSHacks1_8() {}

  @Override
  public void sendPacket(Player bukkitPlayer, Object packet) {
    if (bukkitPlayer.isOnline()) {
      EntityPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
      nmsPlayer.playerConnection.sendPacket((Packet) packet);
    }
  }

  @Override
  public void sendPacketToViewers(Entity entity, Object packet, boolean excludeSpectators) {
    net.minecraft.server.v1_8_R3.Entity nms = ((CraftEntity) entity).getHandle();
    EntityTrackerEntry entry =
        ((WorldServer) nms.getWorld()).getTracker().trackedEntities.get(nms.getId());
    for (EntityPlayer viewer : entry.trackedPlayers) {
      if (excludeSpectators) {
        Entity spectatorTarget = viewer.getBukkitEntity().getSpectatorTarget();
        if (spectatorTarget != null && spectatorTarget.getUniqueId().equals(entity.getUniqueId()))
          continue;
      }
      viewer.playerConnection.sendPacket((Packet) packet);
    }
  }

  static Field entityMetadataWatchableField =
      ReflectionUtils.getField(PacketPlayOutEntityMetadata.class, "b");

  @Override
  public void playDeathAnimation(Player player) {
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

    Object teleport = teleportEntityPacket(player.getEntityId(), location);

    sendPacketToViewers(player, metadata, true);
    sendPacketToViewers(player, useBed, true);
    sendPacketToViewers(player, teleport, true);
  }

  @Override
  public Object teleportEntityPacket(int entityId, Location location) {
    return new PacketPlayOutEntityTeleport(
        entityId, // Entity ID
        (int) (location.getX() * 32), // World X * 32
        (int) (location.getY() * 32), // World Y * 32
        (int) (location.getZ() * 32), // World Z * 32
        (byte) (location.getYaw() * 256 / 360), // Yaw
        (byte) (location.getPitch() * 256 / 360), // Pitch
        true); // On Ground + Height Correction
  }

  @Override
  public Object entityMetadataPacket(int entityId, Entity entity, boolean complete) {
    return new PacketPlayOutEntityMetadata(
        entityId,
        ((CraftEntity) entity).getHandle().getDataWatcher(),
        complete); // true = all values, false = only dirty values
  }

  @Override
  public void skipFireworksLaunch(Firework firework) {
    EntityFireworks entityFirework = ((CraftFirework) firework).getHandle();
    entityFirework.expectedLifespan = 2;
    entityFirework.ticksFlown = 2;
    sendPacketToViewers(
        firework, entityMetadataPacket(firework.getEntityId(), firework, false), false);
  }

  @Override
  public void fakePlayerItemPickup(Player player, Item item) {
    float pitch = (((float) (Math.random() - Math.random()) * 0.7F + 1.0F) * 2.0F);
    item.getWorld().playSound(item.getLocation(), org.bukkit.Sound.ITEM_PICKUP, 0.2F, pitch);

    sendPacketToViewers(
        item, new PacketPlayOutCollect(item.getEntityId(), player.getEntityId()), false);

    item.remove();
  }

  @Override
  public boolean isCraftItemArrowEntity(Item item) {
    return ((CraftItem) item).getHandle() instanceof EntityArrow;
  }

  @Override
  public void freezeEntity(Entity entity) {
    net.minecraft.server.v1_8_R3.Entity nmsEntity = ((CraftEntity) entity).getHandle();
    NBTTagCompound tag = new NBTTagCompound();
    nmsEntity.c(tag); // save to tag
    tag.setBoolean("NoAI", true);
    tag.setBoolean("NoGravity", true);
    nmsEntity.f(tag); // load from tag
  }

  @Override
  public void setFireballDirection(Fireball entity, Vector direction) {
    EntityFireball fireball = ((CraftFireball) entity).getHandle();
    fireball.dirX = direction.getX() * 0.1D;
    fireball.dirY = direction.getY() * 0.1D;
    fireball.dirZ = direction.getZ() * 0.1D;
  }

  @Override
  public void removeAndAddAllTabPlayers(Player viewer) {
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

  @Override
  public void sendLegacyWearing(Player player, int slot, ItemStack item) {
    Packet<?> packet =
        new PacketPlayOutEntityEquipment(
            player.getEntityId(), slot, CraftItemStack.asNMSCopy(item));
    net.minecraft.server.v1_8_R3.Entity nms = ((CraftEntity) player).getHandle();
    EntityTrackerEntry entry =
        ((WorldServer) nms.getWorld()).getTracker().trackedEntities.get(nms.getId());
    for (EntityPlayer viewer : entry.trackedPlayers) {
      if (ViaUtils.getProtocolVersion(viewer.getBukkitEntity()) <= ViaUtils.VERSION_1_7)
        viewer.playerConnection.sendPacket(packet);
    }
  }

  @Override
  public EntityPotion entityPotion(Location location, ItemStack potionItem) {
    return new EntityPotion1_8(location, potionItem);
  }

  @Override
  public void sendBlockChange(Location loc, Player player, @Nullable Material material) {
    if (material != null) player.sendBlockChange(loc, material, (byte) 0);
    else player.sendBlockChange(loc, loc.getBlock().getType(), loc.getBlock().getData());
  }

  @Override
  public void clearArrowsInPlayer(Player player) {
    ((CraftPlayer) player).getHandle().o(0);
  }

  @Override
  public void showBorderWarning(Player player, boolean show) {
    WorldBorder border = new WorldBorder();
    border.setWarningDistance(show ? Integer.MAX_VALUE : 0);
    Object packet =
        new PacketPlayOutWorldBorder(
            border, PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_WARNING_BLOCKS);
    sendPacket(player, packet);
  }

  @Override
  public PotionEffectType getPotionEffectType(String key) {
    MobEffectList nms = MobEffectList.b(key);
    return nms == null ? null : PotionEffectType.getById(nms.id);
  }

  @Override
  public Enchantment getEnchantment(String key) {
    net.minecraft.server.v1_8_R3.Enchantment enchantment =
        net.minecraft.server.v1_8_R3.Enchantment.getByName(key);
    return enchantment == null ? null : org.bukkit.enchantments.Enchantment.getById(enchantment.id);
  }

  @Override
  public long getMonotonicTime(World world) {
    return ((CraftWorld) world).getHandle().getTime();
  }

  @Override
  public int getPing(Player player) {
    return ((CraftPlayer) player).getHandle().ping;
  }

  @Override
  public Object entityEquipmentPacket(int entityId, int slot, ItemStack armor) {
    return new PacketPlayOutEntityEquipment(entityId, slot, CraftItemStack.asNMSCopy(armor));
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

  @Override
  public void entityAttach(Player player, int entityID, int vehicleID, boolean leash) {
    PacketPlayOutAttachEntity packet = new PacketPlayOutAttachEntity();

    ReflectionUtils.setField(packet, (byte) (leash ? 1 : 0), EntityAttachFields.a.getField());
    ReflectionUtils.setField(packet, entityID, EntityAttachFields.b.getField());
    ReflectionUtils.setField(packet, vehicleID, EntityAttachFields.c.getField());

    sendPacket(player, packet);
  }

  @Override
  public Inventory createFakeInventory(Player viewer, Inventory realInventory) {
    return realInventory instanceof DoubleChestInventory
        ? Bukkit.createInventory(viewer, realInventory.getSize())
        : Bukkit.createInventory(viewer, realInventory.getType());
  }

  @Override
  public FakeEntity fakeWitherSkull(World world) {
    return new FakeWitherSkull1_8(world);
  }

  @Override
  public FakeEntity fakeArmorStand(World world, ItemStack head) {
    return new FakeArmorStand1_8(world, head);
  }

  @Override
  public Set<Block> getBlocks(Chunk bukkitChunk, Material material) {
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

  @Override
  public Object spawnPlayerPacket(int entityId, UUID uuid, Location location, Player player) {
    DataWatcher dataWatcher = copyDataWatcher(player);

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
        null == null
            ? 0
            : net.minecraft.server.v1_8_R3.Item.getId(CraftItemStack.asNMSCopy(null).getItem()),
        NamedEntitySpawnFields.h.getField());
    ReflectionUtils.setField(packet, dataWatcher, NamedEntitySpawnFields.i.getField());
    ReflectionUtils.setField(packet, dataWatcher.b(), NamedEntitySpawnFields.j.getField());

    return packet;
  }

  @NotNull
  protected static DataWatcher copyDataWatcher(Player player) {
    DataWatcher original = ((CraftPlayer) player).getHandle().getDataWatcher();
    List<DataWatcher.WatchableObject> values = original.c();
    DataWatcher copy = new DataWatcher(null);
    for (DataWatcher.WatchableObject value : values) {
      copy.a(value.a(), value.b());
    }
    return copy;
  }

  @Override
  public Object destroyEntitiesPacket(int... entityIds) {
    return new PacketPlayOutEntityDestroy(entityIds);
  }

  Field playerInfoActionField = ReflectionUtils.getField(PacketPlayOutPlayerInfo.class, "a");

  @Override
  public Object createPlayerInfoPacket(EnumPlayerInfoAction action) {
    PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
    ReflectionUtils.setField(packet, convertEnumPlayerInfoAction(action), playerInfoActionField);
    return packet;
  }

  static Method enablePotionParticlesMethod = ReflectionUtils.getMethod(EntityLiving.class, "B");
  static Method disablePotionParticlesMethod = ReflectionUtils.getMethod(EntityLiving.class, "bj");

  @Override
  public void setPotionParticles(Player player, boolean enabled) {
    CraftPlayer craftPlayer = (CraftPlayer) player;
    EntityPlayer handle = craftPlayer.getHandle();

    if (enabled) {
      ReflectionUtils.callMethod(enablePotionParticlesMethod, handle);
    } else {
      ReflectionUtils.callMethod(disablePotionParticlesMethod, handle);
    }
  }

  @Override
  public ItemStack craftItemCopy(ItemStack item) {
    return CraftItemStack.asCraftCopy(item);
  }

  @Override
  public RayBlockIntersection getTargetedBLock(Player player) {
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

  Field bField = ReflectionUtils.getField(PacketPlayOutPlayerInfo.class, "b");

  @Override
  public boolean playerInfoDataListNotEmpty(Object packet) {
    List<PacketPlayOutPlayerInfo.PlayerInfoData> result;
    PacketPlayOutPlayerInfo playOutPlayerInfo = (PacketPlayOutPlayerInfo) packet;
    // SportPaper makes this field public
    if (BukkitUtils.isSportPaper()) {
      result = playOutPlayerInfo.b;
    } else {
      result =
          (List<PacketPlayOutPlayerInfo.PlayerInfoData>)
              ReflectionUtils.readField(playOutPlayerInfo, bField);
    }
    return !result.isEmpty();
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

  @Override
  public Object playerListPacketData(
      Object packetPlayOutPlayerInfo,
      UUID uuid,
      String name,
      GameMode gamemode,
      int ping,
      @Nullable Skin skin,
      @Nullable String renderedDisplayName) {
    GameProfile profile = new GameProfile(uuid, name);
    if (skin != null) {
      for (Map.Entry<String, Collection<Property>> entry :
          Skins.toProperties(skin).asMap().entrySet()) {
        profile.getProperties().putAll(entry.getKey(), entry.getValue());
      }
    }

    try {
      WorldSettings.EnumGamemode enumGamemode =
          gamemode == null ? null : WorldSettings.EnumGamemode.getById(gamemode.getValue());
      IChatBaseComponent iChatBaseComponent =
          renderedDisplayName == null
              ? null
              : IChatBaseComponent.ChatSerializer.a(renderedDisplayName);

      return playerInfoDataConstructor.newInstance(
          packetPlayOutPlayerInfo, profile, ping, enumGamemode, iChatBaseComponent);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void addPlayerInfoToPacket(Object packet, Object playerInfoData) {
    List<PacketPlayOutPlayerInfo.PlayerInfoData> result;
    PacketPlayOutPlayerInfo playOutPlayerInfo = (PacketPlayOutPlayerInfo) packet;

    result =
        (List<PacketPlayOutPlayerInfo.PlayerInfoData>)
            ReflectionUtils.readField(playOutPlayerInfo, bField);

    result.add((PacketPlayOutPlayerInfo.PlayerInfoData) playerInfoData);
  }

  Field skullProfileField =
      ReflectionUtils.getField(
          "org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaSkull", "profile");

  @Override
  public void setSkullMetaOwner(SkullMeta meta, String name, UUID uuid, Skin skin) {
    GameProfile gameProfile = new GameProfile(uuid, name);
    Skins.setProperties(skin, gameProfile.getProperties());
    ReflectionUtils.setField(meta, gameProfile, skullProfileField);
  }

  @Override
  public WorldCreator detectWorld(String worldName) {
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

  @Override
  public void setAbsorption(LivingEntity entity, double health) {
    ((CraftLivingEntity) entity).getHandle().setAbsorptionHearts((float) health);
  }

  @Override
  public double getAbsorption(LivingEntity entity) {
    return ((CraftLivingEntity) entity).getHandle().getAbsorptionHearts();
  }

  @Override
  public Set<MaterialData> getBlockStates(Material material) {
    ImmutableSet.Builder<MaterialData> materials = ImmutableSet.builder();
    net.minecraft.server.v1_8_R3.Block nmsBlock = CraftMagicNumbers.getBlock(material);
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

  @Override
  public Skin getPlayerSkin(Player player) {
    CraftPlayer craftPlayer = (CraftPlayer) player;
    return Skins.fromProperties(craftPlayer.getProfile().getProperties());
  }

  @Override
  public void updateVelocity(Player player) {
    EntityPlayer handle = ((CraftPlayer) player).getHandle();
    handle.velocityChanged = false;
    handle.playerConnection.sendPacket(new PacketPlayOutEntityVelocity(handle));
  }

  @Override
  public boolean teleportRelative(
      Player player,
      Vector deltaPos,
      float deltaYaw,
      float deltaPitch,
      PlayerTeleportEvent.TeleportCause cause) {
    CraftPlayer craftPlayer = (CraftPlayer) player;

    if (craftPlayer.getHandle().playerConnection == null
        || craftPlayer.getHandle().playerConnection.isDisconnected()) {
      return false;
    }

    // From = Players current Location
    Location from = player.getLocation();
    // To = Players new Location if Teleport is Successful
    Location to = from.clone().add(deltaPos);
    to.setYaw(to.getYaw() + deltaYaw);
    to.setPitch(to.getPitch() + deltaPitch);

    // Create & Call the Teleport Event.
    PlayerTeleportEvent event = new PlayerTeleportEvent(player, from, to, cause);
    Bukkit.getPluginManager().callEvent(event);

    // Return False to inform the Plugin that the Teleport was unsuccessful/cancelled.
    if (event.isCancelled()) {
      return false;
    }

    craftPlayer.getHandle().playerConnection.teleport(to);
    return true;
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

  @Override
  public void sendSpawnEntityPacket(
      Player player, int entityId, Location location, Vector velocity) {
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
    ReflectionUtils.setField(packet, 66, EntitySpawnFields.j.getField());

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
      Location loc = player.getLocation().subtract(0, 1.1, 0);

      spawnFakeArmorStand(player, entityId, loc, new Vector());
    }
  }

  @Override
  public void spawnFakeArmorStand(Player player, int entityId, Location location, Vector velocity) {
    DataWatcher dataWatcher = new DataWatcher(null);
    int flags = 0;
    flags |= 0x20;
    dataWatcher.a(0, (byte) flags);
    dataWatcher.a(1, (short) 0);
    int flags1 = 0;
    dataWatcher.a(10, (byte) flags1);
    PacketPlayOutSpawnEntityLiving packet = new PacketPlayOutSpawnEntityLiving();

    ReflectionUtils.setField(packet, entityId, LivingEntitySpawnFields.a.getField());
    ReflectionUtils.setField(
        packet, (byte) EntityType.ARMOR_STAND.getTypeId(), LivingEntitySpawnFields.b.getField());
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
    ReflectionUtils.setField(packet, dataWatcher, LivingEntitySpawnFields.l.getField());

    sendPacket(player, packet);
  }

  @Override
  public boolean canMineBlock(MaterialData blockMaterial, ItemStack tool) {
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

  Field worldServerField = ReflectionUtils.getField(CraftWorld.class, "world");
  Field dimensionField = ReflectionUtils.getField(WorldServer.class, "dimension");
  Field modifiersField = ReflectionUtils.getField(Field.class, "modifiers");

  @Override
  public void resetDimension(World world) {
    try {
      modifiersField.setInt(dimensionField, dimensionField.getModifiers() & ~Modifier.FINAL);

      dimensionField.set(worldServerField.get(world), 11);
    } catch (IllegalAccessException e) {
      // No-op, newer version of Java have disabled modifying final fields
    }
  }

  Field unhandledTagsField =
      ReflectionUtils.getField(
          MinecraftReflectionUtils.getCraftBukkitClass("inventory.CraftMetaItem"), "unhandledTags");
  static Field nbtListField = ReflectionUtils.getField(NBTTagList.class, "list");

  @Override
  public Set<Material> getMaterialCollection(ItemMeta itemMeta, String key) {
    Map<String, NBTBase> unhandledTags =
        (Map<String, NBTBase>) ReflectionUtils.readField(itemMeta, unhandledTagsField);
    if (!unhandledTags.containsKey(key)) return EnumSet.noneOf(Material.class);
    EnumSet<Material> materialSet = EnumSet.noneOf(Material.class);
    NBTTagList canDestroyList = (NBTTagList) unhandledTags.get(key);

    for (NBTBase item : (List<NBTBase>) ReflectionUtils.readField(canDestroyList, nbtListField)) {
      NBTTagString nbtTagString = (NBTTagString) item;
      String blockString = nbtTagString.a_();
      materialSet.add(
          Material.getMaterial(
              net.minecraft.server.v1_8_R3.Block.getId(
                  net.minecraft.server.v1_8_R3.Block.getByName(blockString))));
    }

    return materialSet;
  }

  @Override
  public void setMaterialCollection(
      ItemMeta itemMeta, Collection<Material> materials, String canPlaceOn) {
    Map<String, NBTBase> unhandledTags =
        (Map<String, NBTBase>) ReflectionUtils.readField(itemMeta, unhandledTagsField);
    NBTTagList canDestroyList =
        unhandledTags.containsKey(canPlaceOn)
            ? (NBTTagList) unhandledTags.get(canPlaceOn)
            : new NBTTagList();
    for (Material material : materials) {
      net.minecraft.server.v1_8_R3.Block block =
          net.minecraft.server.v1_8_R3.Block.getById(material.getId());
      if (block != null) {
        canDestroyList.add(
            new NBTTagString(net.minecraft.server.v1_8_R3.Block.REGISTRY.c(block).toString()));
      }
    }
    if (!canDestroyList.isEmpty()) unhandledTags.put(canPlaceOn, canDestroyList);
  }

  @Override
  public void setCanDestroy(ItemMeta itemMeta, Collection<Material> materials) {
    setMaterialCollection(itemMeta, materials, "CanDestroy");
  }

  @Override
  public Set<Material> getCanDestroy(ItemMeta itemMeta) {
    return getMaterialCollection(itemMeta, "CanDestroy");
  }

  @Override
  public void setCanPlaceOn(ItemMeta itemMeta, Collection<Material> materials) {
    setMaterialCollection(itemMeta, materials, "CanPlaceOn");
  }

  @Override
  public Set<Material> getCanPlaceOn(ItemMeta itemMeta) {
    return getMaterialCollection(itemMeta, "CanPlaceOn");
  }

  @Override
  public void applyAttributeModifiers(
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

    Map<String, NBTBase> unhandledTags =
        (Map<String, NBTBase>) ReflectionUtils.readField(meta, unhandledTagsField);
    unhandledTags.put("AttributeModifiers", list);
  }

  @Override
  public SetMultimap<String, AttributeModifier> getAttributeModifiers(ItemMeta meta) {
    Map<String, NBTBase> unhandledTags =
        (Map<String, NBTBase>) ReflectionUtils.readField(meta, unhandledTagsField);
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

  @Override
  public double getTPS() {
    return 20.0;
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

    return packet;
  }

  @Override
  public AttributeMap buildAttributeMap(Player player) {
    return new AttributeMap1_8(player);
  }

  @Override
  public void postToMainThread(Plugin plugin, boolean priority, Runnable task) {
    MinecraftServer server = ((CraftServer) plugin.getServer()).getHandle().getServer();
    server.a(Executors.callable(task));
  }

  @NotNull
  protected static PacketPlayOutPlayerInfo.EnumPlayerInfoAction convertEnumPlayerInfoAction(
      EnumPlayerInfoAction action) {
    PacketPlayOutPlayerInfo.EnumPlayerInfoAction nmsAction;
    switch (action) {
      case ADD_PLAYER:
        nmsAction = PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER;
        break;
      case UPDATE_GAME_MODE:
        nmsAction = PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_GAME_MODE;
        break;
      case UPDATE_LATENCY:
        nmsAction = PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_LATENCY;
        break;
      case UPDATE_DISPLAY_NAME:
        nmsAction = PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME;
        break;
      case REMOVE_PLAYER:
      default:
        nmsAction = PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER;
        break;
    }
    return nmsAction;
  }

  public enum NamedEntitySpawnFields {
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
}
