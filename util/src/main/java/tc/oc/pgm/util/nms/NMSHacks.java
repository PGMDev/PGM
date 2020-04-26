package tc.oc.pgm.util.nms;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.*;
import javax.annotation.Nullable;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_8_R3.*;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.WorldBorder;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Skull;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.*;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaBook;
import org.bukkit.craftbukkit.v1_8_R3.potion.CraftPotionEffectType;
import org.bukkit.craftbukkit.v1_8_R3.scoreboard.CraftTeam;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_8_R3.util.Skins;
import org.bukkit.entity.*;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeWrapper;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.reflect.ReflectionUtils;

public interface NMSHacks {

  static EntityTrackerEntry getTrackerEntry(net.minecraft.server.v1_8_R3.Entity nms) {
    return ((WorldServer) nms.getWorld()).getTracker().trackedEntities.get(nms.getId());
  }

  static EntityTrackerEntry getTrackerEntry(Entity entity) {
    return getTrackerEntry(((CraftEntity) entity).getHandle());
  }

  static void forceRespawn(Player player) {
    if (player.isDead()) {
      if (player.getVehicle() != null) {
        player.getVehicle().eject();
      }
      PacketPlayInClientCommand packet =
          new PacketPlayInClientCommand(
              PacketPlayInClientCommand.EnumClientCommand.PERFORM_RESPAWN);
      CraftPlayer cplayer = (CraftPlayer) player;
      packet.a(cplayer.getHandle().playerConnection);
    }
  }

  /** Check all of the given villagers trades and replace any with invalid data */
  static void fixVillagerTrades(Villager villager) {
    EntityVillager nms = ((CraftVillager) villager).getHandle();
    MerchantRecipeList offers = nms.getOffers(null);

    for (int i = 0; i < offers.size(); i++) {
      MerchantRecipe oldRecipe = offers.get(i);
      if (oldRecipe.getBuyItem1() == null || oldRecipe.getBuyItem3() == null) {
        // If the buy1 or sell slots are null, then trying to serialize the
        // villager will generate an NPE. Assume it's a terminal recipe with
        // invalid items, and replace it with a working recipe.
        MerchantRecipe newRecipe =
            new MerchantRecipe(
                new ItemStack(Blocks.BARRIER), // Buy slot 1
                oldRecipe.getBuyItem2(), // Buy slot 2
                new ItemStack(Blocks.BARRIER), // Sell slot
                oldRecipe.e(), // Uses
                oldRecipe.f()); // Max uses
        // Only way to set this field is through NBT
        if (!oldRecipe.j()) {
          NBTTagCompound tag = newRecipe.k();
          tag.setBoolean("rewardExp", false);
          newRecipe.a(tag);
        }

        offers.set(i, newRecipe);
      }
    }
  }

  static void openVillagerTrade(Player bukkitPlayer, Villager bukkitVillager) {
    EntityVillager villager = ((CraftVillager) bukkitVillager).getHandle();
    EntityPlayer player = ((CraftPlayer) bukkitPlayer).getHandle();
    NBTTagCompound data = new NBTTagCompound();
    EntityVillager newVillager = new EntityVillager(player.world);

    villager.b(data);
    newVillager.dead = false;
    newVillager.setAge(1);
    newVillager.a(data);
    newVillager.a_(player);

    player.openTrade(newVillager);
  }

  static boolean hasInfinityEnchanment(Arrow bukkitArrow) {
    EntityArrow arrow = ((CraftArrow) bukkitArrow).getHandle();
    return arrow.fromPlayer == 2; // ELECTROID: EntityArrow.PickupStatus.DISALLOWED;
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

  static PacketPlayOutPlayerInfo.PlayerInfoData playerListPacketData(
      PacketPlayOutPlayerInfo packet,
      UUID uuid,
      String name,
      @Nullable BaseComponent displayName,
      GameMode gamemode,
      int ping,
      @Nullable Skin skin) {
    GameProfile profile = new GameProfile(uuid, name);
    if (skin != null) {
      for (Map.Entry<String, Collection<Property>> entry :
          Skins.toProperties(skin).asMap().entrySet()) {
        profile.getProperties().putAll(entry.getKey(), entry.getValue());
      }
    }
    PacketPlayOutPlayerInfo.PlayerInfoData data =
        packet.constructData(
            profile,
            ping,
            gamemode == null ? null : WorldSettings.EnumGamemode.getById(gamemode.getValue()),
            null); // ELECTROID
    data.displayName = displayName == null ? null : new BaseComponent[] {displayName};
    return data;
  }

  static PacketPlayOutPlayerInfo.PlayerInfoData playerListPacketData(
      PacketPlayOutPlayerInfo packet, UUID uuid, BaseComponent displayName) {
    return playerListPacketData(packet, uuid, null, displayName, null, 0, null);
  }

  static PacketPlayOutPlayerInfo.PlayerInfoData playerListPacketData(
      PacketPlayOutPlayerInfo packet, UUID uuid) {
    return playerListPacketData(packet, uuid, null, null, null, 0, null);
  }

  static Packet playerListAddPacket(
      UUID uuid,
      String name,
      @Nullable BaseComponent displayName,
      GameMode gamemode,
      int ping,
      @Nullable Skin skin) {
    PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
    packet.a = PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER;
    packet.b.add(playerListPacketData(packet, uuid, name, displayName, gamemode, ping, skin));
    return packet;
  }

  static Packet playerListRemovePacket(UUID uuid, String name) {
    PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
    packet.a = PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER;

    packet.b.add(
        new PacketPlayOutPlayerInfo(packet.a)
            .constructData(new GameProfile(uuid, name), 0, null, null));
    return packet;
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

  static Packet teamCreatePacket(
      String name,
      String displayName,
      String prefix,
      String suffix,
      boolean friendlyFire,
      boolean seeFriendlyInvisibles,
      NameTagVisibility nameTagVisibility,
      Collection<String> players) {
    return teamPacket(
        0,
        name,
        displayName,
        prefix,
        suffix,
        friendlyFire,
        seeFriendlyInvisibles,
        nameTagVisibility,
        players);
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
        null,
        Lists.<String>newArrayList());
  }

  static Packet teamJoinPacket(String name, Collection<String> players) {
    return teamPacket(3, name, null, null, null, false, false, null, players);
  }

  static Packet teamLeavePacket(String name, Collection<String> players) {
    return teamPacket(4, name, null, null, null, false, false, null, players);
  }

  /** Entity Spawning and Metadata */
  static int allocateEntityId() {
    return allocateEntityId(null);
  }

  static int allocateEntityId(@Nullable Object context) {
    Integer i = null;
    if (context != null) {
      i = ENTITY_IDS.get(context);
    }
    if (i == null) {
      i = Bukkit.allocateEntityId();
      if (context != null) ENTITY_IDS.put(context, i);
    }
    return i;
  }

  // FIXME: Ensure map size does not grow forever
  Map<Object, Integer> ENTITY_IDS = new WeakHashMap<>();

  static class EntityMetadata {
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

  static EntityMetadata createBossMetadata(String name, float health) {
    EntityMetadata data = createEntityMetadata();
    setEntityMetadata(data, (byte) 0x20, (short) 300);
    setLivingEntityMetadata(data, health, Color.BLACK, false, (byte) 0, name, true, true);
    return data;
  }

  static EntityMetadata createWitherMetadata(String name, float health) {
    EntityMetadata data = createBossMetadata(name, health);
    DataWatcher watcher = data.dataWatcher;
    watcher.a(20, 890); // Invulnerability countdown
    return data;
  }

  static void spawnWither(
      Player player, int entityId, Location location, String name, float health) {
    EntityMetadata data = createWitherMetadata(name, health);
    spawnLivingEntity(player, EntityType.WITHER, entityId, location, data);
  }

  static Packet destroyEntitiesPacket(int... entityIds) {
    return new PacketPlayOutEntityDestroy(entityIds);
  }

  static void destroyEntities(Player player, int... entityIds) {
    sendPacket(player, destroyEntitiesPacket(entityIds));
  }

  static void updateBoss(Player player, int entityId, String name, float health) {
    EntityMetadata data = createBossMetadata(name, health);
    sendPacket(player, new PacketPlayOutEntityMetadata(entityId, data.dataWatcher, true));
  }

  static Packet spawnPlayerPacket(int entityId, UUID uuid, Location location, Player player) {
    return spawnPlayerPacket(entityId, uuid, location, null, EntityMetadata.clone(player));
  }

  static Packet spawnPlayerPacket(
      int entityId,
      UUID uuid,
      Location location,
      org.bukkit.inventory.ItemStack heldItem,
      EntityMetadata metadata) {
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
  }

  static void spawnLivingEntity(
      Player player, EntityType type, int entityId, Location location, EntityMetadata metadata) {
    sendPacket(player, spawnLivingEntityPacket(type, entityId, location, metadata));
  }

  @SuppressWarnings("deprecation")
  static Packet spawnLivingEntityPacket(
      EntityType type, int entityId, Location location, EntityMetadata metadata) {
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
  }

  static void entityAttach(Player player, int entityID, int vehicleID, boolean leash) {
    sendPacket(player, new PacketPlayOutAttachEntity(entityID, vehicleID, leash));
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

  static void teleportEntity(Player player, int entityId, Location location) {
    sendPacket(player, teleportEntityPacket(entityId, location));
  }

  static Packet entityMetadataPacket(Entity entity, boolean complete) {
    return entityMetadataPacket(((CraftEntity) entity).getHandle().getId(), entity, complete);
  }

  static Packet entityMetadataPacket(int entityId, Entity entity, boolean complete) {
    return new PacketPlayOutEntityMetadata(
        entityId,
        ((CraftEntity) entity).getHandle().getDataWatcher(),
        complete); // true = all values, false = only dirty values
  }

  static Packet entityMetadataPacket(int entityId, EntityMetadata metadata, boolean complete) {
    return new PacketPlayOutEntityMetadata(
        entityId, metadata.dataWatcher, complete); // true = all values, false = only dirty values
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

  static void setLivingEntityMetadata(
      EntityMetadata metadata,
      float health,
      Color potionEffectColor,
      boolean potionEffectAmbient,
      byte arrowCount,
      String name,
      boolean showName,
      boolean noAI) {
    DataWatcher dataWatcher = metadata.dataWatcher;
    dataWatcher.a(6, (float) health);
    dataWatcher.a(7, (int) potionEffectColor.asRGB());
    dataWatcher.a(8, (byte) (potionEffectAmbient ? 1 : 0));
    dataWatcher.a(9, (byte) arrowCount);
    dataWatcher.a(2, name);
    dataWatcher.a(3, (byte) (showName ? 1 : 0));
    dataWatcher.a(15, (byte) (noAI ? 1 : 0));
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

  static Packet entityEquipmentPacket(int entityId, int slot, org.bukkit.inventory.ItemStack item) {
    return new PacketPlayOutEntityEquipment(entityId, slot, CraftItemStack.asNMSCopy(item));
  }

  static void playEffect(World bukkitWorld, Vector pos, int effectId, int data) {
    WorldServer world = ((CraftWorld) bukkitWorld).getHandle();
    world.triggerEffect(
        effectId, new BlockPosition(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()), data);
  }

  @SuppressWarnings("deprecation")
  static void playBlockBreakEffect(World bukkitWorld, Vector pos, org.bukkit.Material material) {
    playEffect(bukkitWorld, pos, 2001, material.getId());
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

  /**
   * Test if a {@link Skull} has a cached skin. If this returns false, the skull will likely try to
   * fetch its skin the next time it is loaded.
   */
  static boolean isSkullCached(Skull skull) {
    TileEntitySkull nmsSkull =
        (TileEntitySkull)
            ((CraftWorld) skull.getWorld())
                .getTileEntityAt(skull.getX(), skull.getY(), skull.getZ());
    return nmsSkull.getGameProfile() == null
        || (nmsSkull.getGameProfile().isComplete()
            && nmsSkull.getGameProfile().getProperties().containsKey("textures"));
  }

  static void sendMessage(Player player, BaseComponent[] message, int position) {
    PacketPlayOutChat packet = new PacketPlayOutChat(null, (byte) position);
    packet.components = message;
    sendPacket(player, packet);
  }

  // Only legacy formatting actually works, even though the packet uses components.
  // If this is ever fixed, the methods below can be changed to pass the components through.
  static void sendHotbarMessage(Player player, String message) {
    sendMessage(player, new BaseComponent[] {new PersonalizedText(message).render(player)}, 2);
  }

  static void sendHotbarMessage(Player player, Component message) {
    sendHotbarMessage(player, message.toLegacyText());
  }

  static void enableArmorSlots(ArmorStand armorStand, boolean enabled) {
    CraftArmorStand craftArmorStand = (CraftArmorStand) armorStand;
    NBTTagCompound nbt = new NBTTagCompound();
    craftArmorStand.getHandle().b(nbt);
    nbt.setInt("DisabledSlots", enabled ? 0 : 0x1f1f00);
    craftArmorStand.getHandle().a(nbt);
  }

  static Object particlesPacket(
      String name,
      boolean longRange,
      Vector pos,
      Vector offset,
      float data,
      int count,
      int... extra) {
    return new PacketPlayOutWorldParticles(
        EnumParticle.valueOf(EnumParticle.class, name),
        longRange,
        (float) pos.getX(),
        (float) pos.getY(),
        (float) pos.getZ(),
        (float) offset.getX(),
        (float) offset.getY(),
        (float) offset.getZ(),
        data,
        count,
        extra);
  }

  static Object blockCrackParticlesPacket(
      MaterialData material, boolean longRange, Vector pos, Vector offset, float data, int count) {
    return particlesPacket(
        "BLOCK_CRACK",
        longRange,
        pos,
        offset,
        data,
        count,
        material.getItemTypeId() + (material.getData() << 12));
  }

  static void showBorderWarning(Player player, boolean show) {
    WorldBorder border = new WorldBorder();
    border.setWarningDistance(show ? Integer.MAX_VALUE : 0);
    sendPacket(
        player,
        new PacketPlayOutWorldBorder(
            border, PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_WARNING_BLOCKS));
  }

  static void playDeathAnimation(Player player) {
    EntityPlayer handle = ((CraftPlayer) player).getHandle();
    PacketPlayOutEntityMetadata packet =
        new PacketPlayOutEntityMetadata(handle.getId(), handle.getDataWatcher(), false);

    // Add/replace health to zero
    boolean replaced = false;
    DataWatcher.WatchableObject zeroHealth =
        new DataWatcher.WatchableObject(3, 6, 0f); // type 3 (float), index 6 (health)

    if (packet.b != null) {
      for (int i = 0; i < packet.b.size(); i++) {
        DataWatcher.WatchableObject wo = packet.b.get(i);
        if (wo.a() == 6) {
          packet.b.set(i, zeroHealth);
          replaced = true;
        }
      }
    }

    if (!replaced) {
      if (packet.b == null) {
        packet.b = Collections.singletonList(zeroHealth);
      } else {
        packet.b.add(zeroHealth);
      }
    }

    sendPacketToViewers(player, packet);
  }

  static ItemStack asNMS(org.bukkit.inventory.ItemStack bukkit) {
    if (bukkit instanceof CraftItemStack) {
      return ((CraftItemStack) bukkit).getHandle();
    } else {
      return CraftItemStack.asNMSCopy(bukkit);
    }
  }

  static String getKey(org.bukkit.Material material) {
    MinecraftKey key = Item.REGISTRY.c(Item.getById(material.getId()));
    return key == null ? null : key.toString();
  }

  static String getTranslationKey(org.bukkit.inventory.ItemStack stack) {
    ItemStack nms = asNMS(stack);
    return nms == null ? null : nms.getItem().k(nms) + ".name";
  }

  static String getTranslationKey(Block nmsBlock) {
    return nmsBlock.a() + ".name";
  }

  // Some world cannot be made into NMS ItemStacks (e.g. Lava),
  // so try to make them directly into blocks instead.
  static String getBlockTranslationKey(org.bukkit.Material material) {
    Block nmsBlock = CraftMagicNumbers.getBlock(material);
    return nmsBlock == null ? null : getTranslationKey(nmsBlock);
  }

  static String getTranslationKey(org.bukkit.Material material) {
    String key = getTranslationKey(new org.bukkit.inventory.ItemStack(material));
    return key != null ? key : getBlockTranslationKey(material);
  }

  static String getTranslationKey(MaterialData material) {
    String key = getTranslationKey(material.toItemStack(1));
    return key != null ? key : getBlockTranslationKey(material.getItemType());
  }

  static String getTranslationKey(org.bukkit.entity.Entity entity) {
    net.minecraft.server.v1_8_R3.Entity nms = ((CraftEntity) entity).getHandle();
    String key = EntityTypes.b(nms);
    if (key == null) key = "generic";
    return "entity." + key + ".name";
  }

  static String getTranslationKey(org.bukkit.entity.EntityType entity) {
    return "entity." + entity.getName() + ".name";
  }

  static String getTranslationKey(PotionEffectType effect) {
    if (effect instanceof CraftPotionEffectType) {
      return ((CraftPotionEffectType) effect).getHandle().a();
    } else if (effect instanceof PotionEffectTypeWrapper) {
      return getTranslationKey(((PotionEffectTypeWrapper) effect).getType());
    } else {
      return "potion.empty";
    }
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

  static void setBookPages(BookMeta book, BaseComponent... pages) {
    for (BaseComponent page : pages) {
      ((CraftMetaBook) book)
          .pages.add(IChatBaseComponent.ChatSerializer.a(ComponentSerializer.toString(page)));
    }
  }

  static void openBook(org.bukkit.inventory.ItemStack book, Player player) {
    ((CraftPlayer) player).getHandle().openBook(CraftItemStack.asNMSCopy(book));
  }

  static int getProtocolVersion(Player player) {
    return ((CraftPlayer) player).getHandle().playerConnection.networkManager.protocolVersion;
  }
}
