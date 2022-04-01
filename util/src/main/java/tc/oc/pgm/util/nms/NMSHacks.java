package tc.oc.pgm.util.nms;

// import com.comphenix.protocol.PacketType;
// import com.comphenix.protocol.ProtocolLibrary;
// import com.comphenix.protocol.ProtocolManager;
// import com.comphenix.protocol.events.InternalStructure;
// import com.comphenix.protocol.events.PacketContainer;
// import com.comphenix.protocol.utility.MinecraftReflection;
// import com.comphenix.protocol.wrappers.WrappedChatComponent;
// import java.lang.reflect.InvocationTargetException;
// import java.util.Optional;
// import java.util.logging.Logger;
// import net.kyori.adventure.text.Component;
// import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
// import org.bukkit.Bukkit;
// import org.bukkit.ChatColor;
// import org.bukkit.entity.Player;
// import org.bukkit.scoreboard.Team;

public interface NMSHacks {

  //  ProtocolManager PROTOCOL._MANAGER = ProtocolLibrary.getProtocolManager();
  //  Logger LOGGER = Logger.getLogger("NMSHACKS");
  //
  //  static void sendPacket(PacketContainer packet) {
  //    for (Player pl : Bukkit.getOnlinePlayers()) {
  //      sendPacket(pl, packet);
  //    }
  //  }
  //
  //  static void sendPacket(Player player, PacketContainer packet) {
  //    try {
  //      PROTOCOL_MANAGER.sendServerPacket(player, packet);
  //    } catch (InvocationTargetException e) {
  //      LOGGER.warning(
  //          "Failed to send packet " + packet.toString() + " to player: " + player.getName());
  //    }
  //  }
  //
  //  static PacketContainer teamCreatePacket(
  //      String teamName,
  //      Component displayName,
  //      boolean friendlyFire,
  //      boolean seeFriendlyInvisibles,
  //      Team.OptionStatus nameTagVisibility,
  //      ChatColor teamColor) {
  //    PacketContainer teamPacket = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
  //    teamPacket.getStrings().write(0, teamName);
  //    teamPacket.getIntegers().write(0, 0); // Team create packet
  //
  //    InternalStructure internalStructure = teamPacket.getOptionalStructures().read(0).get();
  //
  //    GsonComponentSerializer gsonSerializer = GsonComponentSerializer.gson();
  //
  //    String serializedDisplayName = gsonSerializer.serialize(displayName);
  //
  //    internalStructure
  //        .getChatComponents()
  //        .write(0, WrappedChatComponent.fromJson(serializedDisplayName)) // Display name
  //        .write(1, WrappedChatComponent.fromText("")) // prefix
  //        .write(2, WrappedChatComponent.fromText("")); // suffix
  //
  //    int friendlyFlags = 0;
  //    if (friendlyFire) {
  //      friendlyFlags |= 1;
  //    }
  //    if (seeFriendlyInvisibles) {
  //      friendlyFlags |= 2;
  //    }
  //
  //    internalStructure.getIntegers().write(0, friendlyFlags);
  //
  //    String visibilityString = "";
  //
  //    switch (nameTagVisibility) {
  //      case ALWAYS:
  //        visibilityString = "always";
  //        break;
  //      case NEVER:
  //        visibilityString = "never";
  //        break;
  //      case FOR_OTHER_TEAMS:
  //        visibilityString = "hideForOtherTeams";
  //        break;
  //      case FOR_OWN_TEAM:
  //        visibilityString = "hideForOwnTeam";
  //        break;
  //    }
  //
  //    String collisionRule = "never";
  //
  //    internalStructure.getStrings().write(0, visibilityString).write(1, collisionRule);
  //
  //    //    internalStructure.get
  //    //    teamColor.ordinal()
  //
  //    internalStructure
  //        .getEnumModifier(ChatColor.class,
  // MinecraftReflection.getMinecraftClass("EnumChatFormat"))
  //        .write(0, teamColor);
  //
  //    teamPacket.getOptionalStructures().write(0, Optional.of(internalStructure));
  //
  //    return null;
  //  }
  //
  //    AtomicInteger ENTITY_IDS = new AtomicInteger(Integer.MAX_VALUE);
  //
  //    static EntityTrackerEntry getTrackerEntry(net.minecraft.server.v1_8_R3.Entity nms) {
  //      return ((WorldServer) nms.getWorld()).getTracker().trackedEntities.get(nms.getId());
  //    }
  //
  //    static EntityTrackerEntry getTrackerEntry(Entity entity) {
  //      return getTrackerEntry(((CraftEntity) entity).getHandle());
  //    }
  //
  //    static void sendPacket(Object packet) {
  //      for (Player pl : Bukkit.getOnlinePlayers()) {
  //        sendPacket(pl, packet);
  //      }
  //    }
  //
  //    static void sendPacket(Player bukkitPlayer, Object packet) {
  //      if (bukkitPlayer.isOnline()) {
  //        EntityPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
  //        nmsPlayer.playerConnection.sendPacket((Packet) packet);
  //      }
  //    }
  //
  //    static void sendPacketToViewers(Entity entity, Object packet) {
  //      sendPacketToViewers(entity, packet, false);
  //    }
  //
  //    static void sendPacketToViewers(Entity entity, Object packet, boolean excludeSpectators) {
  //      EntityTrackerEntry entry = getTrackerEntry(entity);
  //      for (EntityPlayer viewer : ((Set<EntityPlayer>) entry.trackedPlayers)) {
  //        if (excludeSpectators) {
  //          Entity spectatorTarget = viewer.getBukkitEntity().getSpectatorTarget();
  //          if (spectatorTarget != null &&
  //   spectatorTarget.getUniqueId().equals(entity.getUniqueId()))
  //            continue;
  //        }
  //        viewer.playerConnection.sendPacket((Packet) packet);
  //      }
  //    }
  //
  //    /** Immediately send the given entity's metadata to all viewers in range */
  //    static void sendEntityMetadataToViewers(Entity entity, boolean complete) {
  //      sendPacketToViewers(entity, entityMetadataPacket(entity.getEntityId(), entity, complete));
  //    }
  //
  //    Constructor<PacketPlayOutPlayerInfo.PlayerInfoData> playerInfoDataConstructor =
  //        getPlayerInfoDataConstructor();
  //
  //    static Constructor<PacketPlayOutPlayerInfo.PlayerInfoData> getPlayerInfoDataConstructor() {
  //      try {
  //        Constructor<PacketPlayOutPlayerInfo.PlayerInfoData> constructor =
  //            PacketPlayOutPlayerInfo.PlayerInfoData.class.getConstructor(
  //                PacketPlayOutPlayerInfo.class,
  //                GameProfile.class,
  //                int.class,
  //                WorldSettings.EnumGamemode.class,
  //                IChatBaseComponent.class);
  //
  //        constructor.setAccessible(true);
  //        return constructor;
  //      } catch (NoSuchMethodException e) {
  //        throw new RuntimeException(e);
  //      }
  //    }
  //
  //    static PacketPlayOutPlayerInfo.PlayerInfoData playerListPacketData(
  //        PacketPlayOutPlayerInfo packet,
  //        UUID uuid,
  //        String name,
  //        GameMode gamemode,
  //        int ping,
  //        @Nullable Skin skin,
  //        @Nullable String renderedDisplayName) {
  //      GameProfile profile = new GameProfile(uuid, name);
  //      if (skin != null) {
  //        for (Map.Entry<String, Collection<Property>> entry :
  //            Skins.toProperties(skin).asMap().entrySet()) {
  //          profile.getProperties().putAll(entry.getKey(), entry.getValue());
  //        }
  //      }
  //
  //      if (BukkitUtils.isSportPaper()) {
  //        try {
  //          PacketPlayOutPlayerInfo.PlayerInfoData data =
  //              packet.constructData(
  //                  profile,
  //                  ping,
  //                  gamemode == null ? null :
  //   WorldSettings.EnumGamemode.getById(gamemode.getValue()),
  //                  null); // ELECTROID
  //          data.jsonDisplayName = renderedDisplayName;
  //          return data;
  //        } catch (NoSuchFieldError ignored) {
  //        } // Using an old SportPaper version, fallback to spigot reflection
  //      }
  //
  //      try {
  //        WorldSettings.EnumGamemode enumGamemode =
  //            gamemode == null ? null : WorldSettings.EnumGamemode.getById(gamemode.getValue());
  //        IChatBaseComponent iChatBaseComponent =
  //            renderedDisplayName == null
  //                ? null
  //                : IChatBaseComponent.ChatSerializer.a(renderedDisplayName);
  //
  //        return playerInfoDataConstructor.newInstance(
  //            packet, profile, ping, enumGamemode, iChatBaseComponent);
  //      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
  //        throw new RuntimeException(e);
  //      }
  //    }
  //
  //    Field playerInfoActionField = ReflectionUtils.getField(PacketPlayOutPlayerInfo.class, "a");
  //
  //    static PacketPlayOutPlayerInfo createPlayerInfoPacket(
  //        PacketPlayOutPlayerInfo.EnumPlayerInfoAction action) {
  //      PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
  //      if (BukkitUtils.isSportPaper()) {
  //        packet.a = action;
  //      } else {
  //        ReflectionUtils.setField(packet, action, playerInfoActionField);
  //      }
  //      return packet;
  //    }
  //
  //    static PacketPlayOutPlayerInfo.PlayerInfoData playerListPacketData(
  //        PacketPlayOutPlayerInfo packet, UUID uuid, String renderedDisplayName) {
  //      return playerListPacketData(
  //          packet, uuid, "|" + uuid.toString().substring(0, 15), null, 0, null,
  //   renderedDisplayName);
  //    }
  //
  //    static PacketPlayOutPlayerInfo.PlayerInfoData playerListPacketData(
  //        PacketPlayOutPlayerInfo packet, UUID uuid) {
  //      return playerListPacketData(packet, uuid, null, null, 0, null, null);
  //    }
  //
  //    static PacketPlayOutPlayerInfo.PlayerInfoData playerListPacketData(
  //        PacketPlayOutPlayerInfo packet, UUID uuid, int ping) {
  //      return playerListPacketData(
  //          packet, uuid, uuid.toString().substring(0, 16), null, ping, null, null);
  //    }
  //
  //    /**
  //     * Removes all players from the tab for the viewer and re-adds them
  //     *
  //     * @param viewer The viewer to send the packets to
  //     */
  //    static void removeAndAddAllTabPlayers(Player viewer) {
  //      List<EntityPlayer> players = new ArrayList<>();
  //      for (Player player : Bukkit.getOnlinePlayers()) {
  //        if (viewer.canSee(player) || player == viewer)
  //          players.add(((CraftPlayer) player).getHandle());
  //      }
  //
  //      sendPacket(
  //          viewer,
  //          new PacketPlayOutPlayerInfo(
  //              PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, players));
  //      sendPacket(
  //          viewer,
  //          new PacketPlayOutPlayerInfo(
  //              PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, players));
  //    }
  //
  //    Field bField = ReflectionUtils.getField(PacketPlayOutPlayerInfo.class, "b");
  //
  //    static List<PacketPlayOutPlayerInfo.PlayerInfoData> getPlayerInfoDataList(
  //        PacketPlayOutPlayerInfo packet) {
  //      // SportPaper makes this field public
  //      if (BukkitUtils.isSportPaper()) {
  //        return packet.b;
  //      } else {
  //        return (List<PacketPlayOutPlayerInfo.PlayerInfoData>)
  //            ReflectionUtils.readField(packet, bField);
  //      }
  //    }
  //
  //    enum TeamPacketFields {
  //      a,
  //      b,
  //      c,
  //      d,
  //      e,
  //      g,
  //      h,
  //      i;
  //
  //      Field field;
  //
  //      TeamPacketFields() {
  //        field = ReflectionUtils.getField(PacketPlayOutScoreboardTeam.class, name());
  //      }
  //
  //      public Field getField() {
  //        return field;
  //      }
  //    }
  //
  //    static Packet teamPacket(
  //        int operation,
  //        String name,
  //        String displayName,
  //        String prefix,
  //        String suffix,
  //        boolean friendlyFire,
  //        boolean seeFriendlyInvisibles,
  //        NameTagVisibility nameTagVisibility,
  //        Collection<String> players) {
  //
  //      PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();
  //
  //      if (BukkitUtils.isSportPaper()) {
  //        packet.a = name;
  //        packet.b = displayName;
  //        packet.c = prefix;
  //        packet.d = suffix;
  //        packet.e = nameTagVisibility == null ? null :
  //   CraftTeam.bukkitToNotch(nameTagVisibility).e;
  //        // packet.f = color
  //        packet.g = players;
  //        packet.h = operation;
  //        if (friendlyFire) {
  //          packet.i |= 1;
  //        }
  //        if (seeFriendlyInvisibles) {
  //          packet.i |= 2;
  //        }
  //      } else {
  //        ReflectionUtils.setField(packet, name, TeamPacketFields.a.getField());
  //        ReflectionUtils.setField(packet, displayName, TeamPacketFields.b.getField());
  //        ReflectionUtils.setField(packet, prefix, TeamPacketFields.c.getField());
  //        ReflectionUtils.setField(packet, suffix, TeamPacketFields.d.getField());
  //
  //        String e = null;
  //        if (nameTagVisibility != null) {
  //          switch (nameTagVisibility) {
  //            case ALWAYS:
  //              e = "always";
  //              break;
  //            case NEVER:
  //              e = "never";
  //              break;
  //            case HIDE_FOR_OTHER_TEAMS:
  //              e = "hideForOtherTeams";
  //              break;
  //            case HIDE_FOR_OWN_TEAM:
  //              e = "hideForOwnTeam";
  //              break;
  //          }
  //        }
  //
  //        ReflectionUtils.setField(packet, e, TeamPacketFields.e.getField());
  //        ReflectionUtils.setField(packet, players, TeamPacketFields.g.getField());
  //        ReflectionUtils.setField(packet, operation, TeamPacketFields.h.getField());
  //
  //        int i = (int) ReflectionUtils.readField(packet, TeamPacketFields.i.getField());
  //        if (friendlyFire) {
  //          i |= 1;
  //        }
  //        if (seeFriendlyInvisibles) {
  //          i |= 2;
  //        }
  //
  //        ReflectionUtils.setField(packet, i, TeamPacketFields.i.getField());
  //      }
  //      return packet;
  //    }
  //
  //    static Packet teamCreatePacket(
  //        String name,
  //        String displayName,
  //        String prefix,
  //        String suffix,
  //        boolean friendlyFire,
  //        boolean seeFriendlyInvisibles,
  //        Collection<String> players) {
  //      return teamPacket(
  //          0,
  //          name,
  //          displayName,
  //          prefix,
  //          suffix,
  //          friendlyFire,
  //          seeFriendlyInvisibles,
  //          NameTagVisibility.ALWAYS,
  //          players);
  //    }
  //
  //    static Packet teamRemovePacket(String name) {
  //      return teamPacket(1, name, null, null, null, false, false, null,
  //   Lists.<String>newArrayList());
  //    }
  //
  //    static Packet teamUpdatePacket(
  //        String name,
  //        String displayName,
  //        String prefix,
  //        String suffix,
  //        boolean friendlyFire,
  //        boolean seeFriendlyInvisibles) {
  //      return teamPacket(
  //          2,
  //          name,
  //          displayName,
  //          prefix,
  //          suffix,
  //          friendlyFire,
  //          seeFriendlyInvisibles,
  //          NameTagVisibility.ALWAYS,
  //          Lists.newArrayList());
  //    }
  //
  //    static Packet teamJoinPacket(String name, Collection<String> players) {
  //      return teamPacket(3, name, null, null, null, false, false, null, players);
  //    }
  //
  //    static Packet teamLeavePacket(String name, Collection<String> players) {
  //      return teamPacket(4, name, null, null, null, false, false, null, players);
  //    }
  //
  //    static int allocateEntityId() {
  //      return ENTITY_IDS.decrementAndGet();
  //    }
  //
  //    static void sendLegacyWearing(Player player, int slot, ItemStack item) {
  //      Packet<?> packet =
  //          new PacketPlayOutEntityEquipment(
  //              player.getEntityId(), slot, CraftItemStack.asNMSCopy(item));
  //      EntityTrackerEntry entry = getTrackerEntry(player);
  //      for (EntityPlayer viewer : entry.trackedPlayers) {
  //        if (ViaUtils.getProtocolVersion(viewer.getBukkitEntity()) <= ViaUtils.VERSION_1_7)
  //          viewer.playerConnection.sendPacket(packet);
  //      }
  //    }
  //
  //    class EntityMetadata {
  //      public final DataWatcher dataWatcher;
  //
  //      public EntityMetadata(DataWatcher watcher) {
  //        dataWatcher = watcher;
  //      }
  //
  //      static EntityMetadata clone(DataWatcher original) {
  //        List<DataWatcher.WatchableObject> values = original.c();
  //        DataWatcher copy = new DataWatcher(null);
  //        for (DataWatcher.WatchableObject value : values) {
  //          copy.a(value.a(), value.b());
  //        }
  //        return new EntityMetadata(copy);
  //      }
  //
  //      static EntityMetadata clone(Entity entity) {
  //        return clone(((CraftEntity) entity).getHandle().getDataWatcher());
  //      }
  //
  //      @Override
  //      public EntityMetadata clone() {
  //        return clone(this.dataWatcher);
  //      }
  //    }
  //
  //    static Packet destroyEntitiesPacket(int... entityIds) {
  //      return new PacketPlayOutEntityDestroy(entityIds);
  //    }
  //
  //    static void destroyEntities(Player player, int... entityIds) {
  //      sendPacket(player, destroyEntitiesPacket(entityIds));
  //    }
  //
  //    static Packet spawnPlayerPacket(int entityId, UUID uuid, Location location, Player player) {
  //      return spawnPlayerPacket(entityId, uuid, location, null, EntityMetadata.clone(player));
  //    }
  //
  //    enum NamedEntitySpawnFields {
  //      a,
  //      b,
  //      c,
  //      d,
  //      e,
  //      f,
  //      g,
  //      h,
  //      i,
  //      j;
  //
  //      Field field;
  //
  //      NamedEntitySpawnFields() {
  //        field = ReflectionUtils.getField(PacketPlayOutNamedEntitySpawn.class, name());
  //      }
  //
  //      public Field getField() {
  //        return field;
  //      }
  //    }
  //
  //    static Packet spawnPlayerPacket(
  //        int entityId, UUID uuid, Location location, ItemStack heldItem, EntityMetadata metadata)
  // {
  //      if (BukkitUtils.isSportPaper()) {
  //        return new PacketPlayOutNamedEntitySpawn(
  //            entityId,
  //            uuid,
  //            location.getX(),
  //            location.getY(),
  //            location.getZ(),
  //            (byte) location.getYaw(),
  //            (byte) location.getPitch(),
  //            CraftItemStack.asNMSCopy(heldItem),
  //            metadata.dataWatcher);
  //      } else {
  //        PacketPlayOutNamedEntitySpawn packet = new PacketPlayOutNamedEntitySpawn();
  //
  //        ReflectionUtils.setField(packet, entityId, NamedEntitySpawnFields.a.getField());
  //        ReflectionUtils.setField(packet, uuid, NamedEntitySpawnFields.b.getField());
  //        ReflectionUtils.setField(
  //            packet, MathHelper.floor(location.getX() * 32.0D),
  //   NamedEntitySpawnFields.c.getField());
  //        ReflectionUtils.setField(
  //            packet, MathHelper.floor(location.getY() * 32.0D),
  //   NamedEntitySpawnFields.d.getField());
  //        ReflectionUtils.setField(
  //            packet, MathHelper.floor(location.getZ() * 32.0D),
  //   NamedEntitySpawnFields.e.getField());
  //        ReflectionUtils.setField(
  //            packet,
  //            (byte) ((int) (((byte) location.getYaw()) * 256.0F / 360.0F)),
  //            NamedEntitySpawnFields.f.getField());
  //        ReflectionUtils.setField(
  //            packet,
  //            (byte) ((int) (((byte) location.getPitch()) * 256.0F / 360.0F)),
  //            NamedEntitySpawnFields.g.getField());
  //        ReflectionUtils.setField(
  //            packet,
  //            heldItem == null ? 0 : Item.getId(CraftItemStack.asNMSCopy(heldItem).getItem()),
  //            NamedEntitySpawnFields.h.getField());
  //        ReflectionUtils.setField(packet, metadata.dataWatcher,
  //   NamedEntitySpawnFields.i.getField());
  //        ReflectionUtils.setField(
  //            packet, metadata.dataWatcher.b(), NamedEntitySpawnFields.j.getField());
  //
  //        return packet;
  //      }
  //    }
  //
  //    static void spawnLivingEntity(
  //        Player player, EntityType type, int entityId, Location location, EntityMetadata
  // metadata)
  //   {
  //      sendPacket(player, spawnLivingEntityPacket(type, entityId, location, metadata));
  //    }
  //
  //    enum LivingEntitySpawnFields {
  //      a,
  //      b,
  //      c,
  //      d,
  //      e,
  //      i,
  //      j,
  //      k,
  //      l;
  //
  //      Field field;
  //
  //      LivingEntitySpawnFields() {
  //        field = ReflectionUtils.getField(PacketPlayOutSpawnEntityLiving.class, name());
  //      }
  //
  //      public Field getField() {
  //        return field;
  //      }
  //    }
  //
  //    @SuppressWarnings("deprecation")
  //    static Packet spawnLivingEntityPacket(
  //        EntityType type, int entityId, Location location, EntityMetadata metadata) {
  //      if (BukkitUtils.isSportPaper()) {
  //        return new PacketPlayOutSpawnEntityLiving(
  //            entityId,
  //            (byte) type.getTypeId(),
  //            location.getX(),
  //            location.getY(),
  //            location.getZ(),
  //            location.getYaw(),
  //            location.getPitch(),
  //            location.getPitch(),
  //            0,
  //            0,
  //            0,
  //            metadata.dataWatcher);
  //      } else {
  //        PacketPlayOutSpawnEntityLiving packet = new PacketPlayOutSpawnEntityLiving();
  //
  //        ReflectionUtils.setField(packet, entityId, LivingEntitySpawnFields.a.getField());
  //        ReflectionUtils.setField(
  //            packet, (byte) type.getTypeId(), LivingEntitySpawnFields.b.getField());
  //        ReflectionUtils.setField(
  //            packet, MathHelper.floor(location.getX() * 32.0D),
  //   LivingEntitySpawnFields.c.getField());
  //        ReflectionUtils.setField(
  //            packet, MathHelper.floor(location.getY() * 32.0D),
  //   LivingEntitySpawnFields.d.getField());
  //        ReflectionUtils.setField(
  //            packet, MathHelper.floor(location.getZ() * 32.0D),
  //   LivingEntitySpawnFields.e.getField());
  //        ReflectionUtils.setField(
  //            packet,
  //            (byte) ((int) (((byte) location.getYaw()) * 256.0F / 360.0F)),
  //            LivingEntitySpawnFields.i.getField());
  //        ReflectionUtils.setField(
  //            packet,
  //            (byte) ((int) (((byte) location.getPitch()) * 256.0F / 360.0F)),
  //            LivingEntitySpawnFields.j.getField());
  //        ReflectionUtils.setField(
  //            packet,
  //            (byte) ((int) (((byte) location.getPitch()) * 256.0F / 360.0F)),
  //            LivingEntitySpawnFields.k.getField());
  //        ReflectionUtils.setField(packet, metadata.dataWatcher,
  //   LivingEntitySpawnFields.l.getField());
  //
  //        return packet;
  //      }
  //    }
  //
  //    static void spawnEntity(Player player, int type, int entityId, Location location) {
  //      sendPacket(player, spawnEntityPacket(type, entityId, location));
  //    }
  //
  //    enum EntitySpawnFields {
  //      a,
  //      b,
  //      c,
  //      d,
  //      h,
  //      i,
  //      j;
  //
  //      Field field;
  //
  //      EntitySpawnFields() {
  //        field = ReflectionUtils.getField(PacketPlayOutSpawnEntity.class, name());
  //      }
  //
  //      public Field getField() {
  //        return field;
  //      }
  //    }
  //
  //    static Packet spawnEntityPacket(int type, int entityId, Location location) {
  //      if (BukkitUtils.isSportPaper()) {
  //        return new PacketPlayOutSpawnEntity(
  //            entityId,
  //            location.getX(),
  //            location.getY(),
  //            location.getZ(),
  //            0,
  //            0,
  //            0,
  //            (int) location.getPitch(),
  //            (int) location.getYaw(),
  //            type,
  //            0);
  //      } else {
  //        PacketPlayOutSpawnEntity packet = new PacketPlayOutSpawnEntity();
  //
  //        ReflectionUtils.setField(packet, entityId, EntitySpawnFields.a.getField());
  //        ReflectionUtils.setField(
  //            packet, MathHelper.floor(location.getX() * 32.0D), EntitySpawnFields.b.getField());
  //        ReflectionUtils.setField(
  //            packet, MathHelper.floor(location.getY() * 32.0D), EntitySpawnFields.c.getField());
  //        ReflectionUtils.setField(
  //            packet, MathHelper.floor(location.getZ() * 32.0D), EntitySpawnFields.d.getField());
  //        ReflectionUtils.setField(
  //            packet,
  //            (byte) ((int) (((byte) location.getYaw()) * 256.0F / 360.0F)),
  //            EntitySpawnFields.h.getField());
  //        ReflectionUtils.setField(
  //            packet,
  //            (byte) ((int) (((byte) location.getPitch()) * 256.0F / 360.0F)),
  //            EntitySpawnFields.i.getField());
  //        ReflectionUtils.setField(packet, type, EntitySpawnFields.j.getField());
  //
  //        return packet;
  //      }
  //    }
  //
  //    static void spawnFreezeEntity(Player player, int entityId, boolean legacy) {
  //      if (legacy) {
  //        Location location = player.getLocation().add(0, 0.286, 0);
  //        if (location.getY() < -64) {
  //          location.setY(-64);
  //          player.teleport(location);
  //        }
  //
  //        NMSHacks.spawnEntity(player, 66, entityId, location);
  //      } else {
  //        Location loc = player.getLocation().subtract(0, 1.1, 0);
  //
  //        NMSHacks.EntityMetadata metadata = NMSHacks.createEntityMetadata();
  //        NMSHacks.setEntityMetadata(metadata, false, false, false, false, true, (short) 0);
  //        NMSHacks.setArmorStandFlags(metadata, false, false, false, false);
  //        NMSHacks.spawnLivingEntity(player, EntityType.ARMOR_STAND, entityId, loc, metadata);
  //      }
  //    }
  //
  //    enum EntityAttachFields {
  //      a,
  //      b,
  //      c;
  //
  //      Field field;
  //
  //      EntityAttachFields() {
  //        field = ReflectionUtils.getField(PacketPlayOutAttachEntity.class, name());
  //      }
  //
  //      public Field getField() {
  //        return field;
  //      }
  //    }
  //
  //    static void entityAttach(Player player, int entityID, int vehicleID, boolean leash) {
  //      if (BukkitUtils.isSportPaper()) {
  //        sendPacket(player, new PacketPlayOutAttachEntity(entityID, vehicleID, leash));
  //      } else {
  //        PacketPlayOutAttachEntity packet = new PacketPlayOutAttachEntity();
  //
  //        ReflectionUtils.setField(packet, (byte) (leash ? 1 : 0),
  // EntityAttachFields.a.getField());
  //        ReflectionUtils.setField(packet, entityID, EntityAttachFields.b.getField());
  //        ReflectionUtils.setField(packet, vehicleID, EntityAttachFields.c.getField());
  //
  //        sendPacket(player, packet);
  //      }
  //    }
  //
  //    static Packet teleportEntityPacket(int entityId, Location location) {
  //      return new PacketPlayOutEntityTeleport(
  //          entityId, // Entity ID
  //          (int) (location.getX() * 32), // World X * 32
  //          (int) (location.getY() * 32), // World Y * 32
  //          (int) (location.getZ() * 32), // World Z * 32
  //          (byte) (location.getYaw() * 256 / 360), // Yaw
  //          (byte) (location.getPitch() * 256 / 360), // Pitch
  //          true); // On Ground + Height Correction
  //    }
  //
  //    static Packet entityMetadataPacket(int entityId, Entity entity, boolean complete) {
  //      return new PacketPlayOutEntityMetadata(
  //          entityId,
  //          ((CraftEntity) entity).getHandle().getDataWatcher(),
  //          complete); // true = all values, false = only dirty values
  //    }
  //
  //    static EntityMetadata createEntityMetadata() {
  //      return new EntityMetadata(new DataWatcher(null));
  //    }
  //
  //    static void setEntityMetadata(EntityMetadata metadata, byte flags, short air) {
  //      DataWatcher dataWatcher = metadata.dataWatcher;
  //      dataWatcher.a(0, (byte) flags);
  //      dataWatcher.a(1, (short) air);
  //    }
  //
  //    static void setEntityMetadata(
  //        EntityMetadata metadata,
  //        boolean onFire,
  //        boolean crouched,
  //        boolean sprinting,
  //        boolean eatingOrBlocking,
  //        boolean invisible,
  //        short air) {
  //      int flags = 0;
  //      if (onFire) flags |= 0x01;
  //      if (crouched) flags |= 0x02;
  //      if (sprinting) flags |= 0x08;
  //      if (eatingOrBlocking) flags |= 0x10;
  //      if (invisible) flags |= 0x20;
  //      setEntityMetadata(metadata, (byte) flags, air);
  //    }
  //
  //    static void setArmorStandFlags(
  //        EntityMetadata metadata, boolean small, boolean gravity, boolean arms, boolean
  // baseplate)
  //   {
  //      int flags = 0;
  //      if (small) flags |= 0x01;
  //      if (gravity) flags |= 0x02;
  //      if (arms) flags |= 0x04;
  //      if (baseplate) flags |= 0x08;
  //      metadata.dataWatcher.a(10, (byte) flags);
  //    }
  //
  //    Method enablePotionParticlesMethod = ReflectionUtils.getMethod(EntityLiving.class, "B");
  //    Method disablePotionParticlesMethod = ReflectionUtils.getMethod(EntityLiving.class, "bj");
  //
  //    static void setPotionParticles(Player player, boolean enabled) {
  //      if (BukkitUtils.isSportPaper()) {
  //        player.setPotionParticles(enabled);
  //      } else {
  //        CraftPlayer craftPlayer = (CraftPlayer) player;
  //        EntityPlayer handle = craftPlayer.getHandle();
  //
  //        if (enabled) {
  //          ReflectionUtils.callMethod(enablePotionParticlesMethod, handle);
  //        } else {
  //          ReflectionUtils.callMethod(disablePotionParticlesMethod, handle);
  //        }
  //      }
  //    }
  //
  //    /**
  //     * Test if the given tool is capable of "efficiently" mining the given block.
  //     *
  //     * <p>Derived from CraftBlock.itemCausesDrops()
  //     */
  //    static boolean canMineBlock(MaterialData blockMaterial, ItemStack tool) {
  //      if (!blockMaterial.getItemType().isBlock()) {
  //        throw new IllegalArgumentException("Material '" + blockMaterial + "' is not a block");
  //      }
  //
  //      net.minecraft.server.v1_8_R3.Block nmsBlock =
  //          CraftMagicNumbers.getBlock(blockMaterial.getItemType());
  //      net.minecraft.server.v1_8_R3.Item nmsTool =
  //          tool == null ? null : CraftMagicNumbers.getItem(tool.getType());
  //
  //      return nmsBlock != null
  //          && (nmsBlock.getMaterial().isAlwaysDestroyable()
  //              || (nmsTool != null && nmsTool.canDestroySpecialBlock(nmsBlock)));
  //    }
  //
  //    static long getMonotonicTime(World world) {
  //      return ((CraftWorld) world).getHandle().getTime();
  //    }
  //
  //    static void sendMessage(Player player, BaseComponent[] message, int position) {
  //      PacketPlayOutChat packet = new PacketPlayOutChat(null, (byte) position);
  //      packet.components = message;
  //      sendPacket(player, packet);
  //    }
  //
  //    static void showBorderWarning(Player player, boolean show) {
  //      WorldBorder border = new WorldBorder();
  //      border.setWarningDistance(show ? Integer.MAX_VALUE : 0);
  //      sendPacket(
  //          player,
  //          new PacketPlayOutWorldBorder(
  //              border, PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_WARNING_BLOCKS));
  //    }
  //
  //    Field entityMetadataWatchableField =
  //        ReflectionUtils.getField(PacketPlayOutEntityMetadata.class, "b");
  //
  //    static void playDeathAnimation(Player player) {
  //      EntityPlayer handle = ((CraftPlayer) player).getHandle();
  //      PacketPlayOutEntityMetadata metadata =
  //          new PacketPlayOutEntityMetadata(handle.getId(), handle.getDataWatcher(), false);
  //
  //      // Add/replace health to zero
  //      boolean replaced = false;
  //      DataWatcher.WatchableObject zeroHealth =
  //          new DataWatcher.WatchableObject(3, 6, 0f); // type 3 (float), index 6 (health)
  //
  //      List<DataWatcher.WatchableObject> b =
  //          (List<DataWatcher.WatchableObject>)
  //              ReflectionUtils.readField(metadata, entityMetadataWatchableField);
  //      if (b != null) {
  //        for (int i = 0; i < b.size(); i++) {
  //          DataWatcher.WatchableObject wo = b.get(i);
  //          if (wo.a() == 6) {
  //            b.set(i, zeroHealth);
  //            replaced = true;
  //          }
  //        }
  //      }
  //
  //      if (!replaced) {
  //        if (b != null) b.add(zeroHealth);
  //        else
  //          ReflectionUtils.setField(
  //              metadata, Collections.singletonList(zeroHealth), entityMetadataWatchableField);
  //      }
  //
  //      Location location = player.getLocation();
  //      PacketPlayOutBed useBed =
  //          new PacketPlayOutBed(
  //              ((CraftPlayer) player).getHandle(),
  //              new BlockPosition(location.getX(), location.getY(), location.getZ()));
  //
  //      Packet<?> teleport = teleportEntityPacket(player.getEntityId(), location);
  //
  //      sendPacketToViewers(player, metadata, true);
  //      sendPacketToViewers(player, useBed, true);
  //      sendPacketToViewers(player, teleport, true);
  //    }
  //
  //    static Packet entityEquipmentPacket(int entityId, int slot, ItemStack armor) {
  //      return new PacketPlayOutEntityEquipment(entityId, slot, CraftItemStack.asNMSCopy(armor));
  //    }
  //
  //    static Skin getPlayerSkin(Player player) {
  //      CraftPlayer craftPlayer = (CraftPlayer) player;
  //      return Skins.fromProperties(craftPlayer.getProfile().getProperties());
  //    }
  //
  //    static void updateVelocity(Player player) {
  //      EntityPlayer handle = ((CraftPlayer) player).getHandle();
  //      handle.velocityChanged = false;
  //      handle.playerConnection.sendPacket(new PacketPlayOutEntityVelocity(handle));
  //    }
  //
  //    static boolean teleportRelative(
  //        Player player,
  //        org.bukkit.util.Vector deltaPos,
  //        float deltaYaw,
  //        float deltaPitch,
  //        PlayerTeleportEvent.TeleportCause cause) {
  //      CraftPlayer craftPlayer = (CraftPlayer) player;
  //
  //      if (craftPlayer.getHandle().playerConnection == null
  //          || craftPlayer.getHandle().playerConnection.isDisconnected()) {
  //        return false;
  //      }
  //
  //      // From = Players current Location
  //      Location from = player.getLocation();
  //      // To = Players new Location if Teleport is Successful
  //      Location to = from.clone().add(deltaPos);
  //      to.setYaw(to.getYaw() + deltaYaw);
  //      to.setPitch(to.getPitch() + deltaPitch);
  //
  //      // Create & Call the Teleport Event.
  //      PlayerTeleportEvent event = new PlayerTeleportEvent(player, from, to, cause);
  //      Bukkit.getPluginManager().callEvent(event);
  //
  //      // Return False to inform the Plugin that the Teleport was unsuccessful/cancelled.
  //      if (event.isCancelled()) {
  //        return false;
  //      }
  //
  //      craftPlayer.getHandle().playerConnection.teleport(to);
  //      return true;
  //    }
  //
  //    Field skullProfileField =
  //        ReflectionUtils.getField(
  //            "org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaSkull", "profile");
  //
  //    static void setSkullMetaOwner(SkullMeta meta, String name, UUID uuid, Skin skin) {
  //      GameProfile gameProfile = new GameProfile(uuid, name);
  //      Skins.setProperties(skin, gameProfile.getProperties());
  //      ReflectionUtils.setField(meta, gameProfile, skullProfileField);
  //    }
  //
  //    static Set<org.bukkit.block.Block> getBlocks(Chunk bukkitChunk, Material material) {
  //      CraftChunk craftChunk = (CraftChunk) bukkitChunk;
  //      Set<org.bukkit.block.Block> blocks = new HashSet<>();
  //
  //      net.minecraft.server.v1_8_R3.Block nmsBlock = CraftMagicNumbers.getBlock(material);
  //      net.minecraft.server.v1_8_R3.Chunk chunk = craftChunk.getHandle();
  //
  //      for (ChunkSection section : chunk.getSections()) {
  //        if (section == null || section.a()) continue; // ChunkSection.a() -> true if section is
  //   empty
  //
  //        char[] blockIds = section.getIdArray();
  //        for (int i = 0; i < blockIds.length; i++) {
  //          // This does a lookup in the block registry, but does not create any objects, so
  // should
  //   be
  //          // pretty efficient
  //          IBlockData blockData = (IBlockData)
  // net.minecraft.server.v1_8_R3.Block.d.a(blockIds[i]);
  //          if (blockData != null && blockData.getBlock() == nmsBlock) {
  //            blocks.add(
  //                bukkitChunk.getBlock(i & 0xf, section.getYPosition() | (i >> 8), (i >> 4) &
  // 0xf));
  //          }
  //        }
  //      }
  //
  //      return blocks;
  //    }
  //
  //    static WorldCreator detectWorld(String worldName) {
  //      IDataManager sdm =
  //          new ServerNBTManager(Bukkit.getServer().getWorldContainer(), worldName, true);
  //      WorldData worldData = sdm.getWorldData();
  //      if (worldData == null) return null;
  //
  //      return new WorldCreator(worldName)
  //          .generateStructures(worldData.shouldGenerateMapFeatures())
  //          .generatorSettings(worldData.getGeneratorOptions())
  //          .seed(worldData.getSeed())
  //          .type(org.bukkit.WorldType.getByName(worldData.getType().name()));
  //    }
  //
  //    Field worldServerField = ReflectionUtils.getField(CraftWorld.class, "world");
  //    Field dimensionField = ReflectionUtils.getField(WorldServer.class, "dimension");
  //    Field modifiersField = ReflectionUtils.getField(Field.class, "modifiers");
  //
  //    static void resetDimension(World world) {
  //      try {
  //        modifiersField.setInt(dimensionField, dimensionField.getModifiers() & ~Modifier.FINAL);
  //
  //        dimensionField.set(worldServerField.get(world), 11);
  //      } catch (IllegalAccessException e) {
  //        // No-op, newer version of Java have disabled modifying final fields
  //      }
  //    }
  //
  //
  //    static void sendBlockChange(Location loc, Player player, @Nullable Material material) {
  //      if (material != null) player.sendBlockChange(loc, material, (byte) 0);
  //      else player.sendBlockChange(loc, loc.getBlock().getType(), loc.getBlock().getData());
  //    }
  //
  //    interface FakeEntity {
  //      int entityId();
  //
  //      Entity entity();
  //
  //      void spawn(Player viewer, Location location, org.bukkit.util.Vector velocity);
  //
  //      default void spawn(Player viewer, Location location) {
  //        spawn(viewer, location, new org.bukkit.util.Vector(0, 0, 0));
  //      }
  //
  //      default void destroy(Player viewer) {
  //        sendPacket(viewer, destroyEntitiesPacket(entityId()));
  //      }
  //
  //      default void teleport(Player viewer, Location location) {
  //        sendPacket(viewer, teleportEntityPacket(entityId(), location));
  //      }
  //
  //      default void ride(Player viewer, Entity rider) {
  //        entityAttach(viewer, rider.getEntityId(), entityId(), false);
  //      }
  //
  //      default void mount(Player viewer, Entity vehicle) {
  //        entityAttach(viewer, entityId(), vehicle.getEntityId(), false);
  //      }
  //
  //      default void wear(Player viewer, int slot, ItemStack item) {
  //        sendPacket(viewer, entityEquipmentPacket(entityId(), slot, item));
  //      }
  //    }
  //
  //    abstract class FakeEntityImpl<T extends net.minecraft.server.v1_8_R3.Entity>
  //        implements FakeEntity {
  //      protected final T entity;
  //
  //      protected FakeEntityImpl(T entity) {
  //        this.entity = entity;
  //      }
  //
  //      @Override
  //      public Entity entity() {
  //        return entity.getBukkitEntity();
  //      }
  //
  //      @Override
  //      public void spawn(Player viewer, Location location, Vector velocity) {
  //        entity.setPositionRotation(
  //            location.getX(),
  //            location.getY(),
  //            location.getZ(),
  //            location.getYaw(),
  //            location.getPitch());
  //        entity.motX = velocity.getX();
  //        entity.motY = velocity.getY();
  //        entity.motZ = velocity.getZ();
  //        sendPacket(viewer, spawnPacket());
  //      }
  //
  //      abstract Packet<?> spawnPacket();
  //
  //      @Override
  //      public int entityId() {
  //        return entity.getId();
  //      }
  //    }
  //
  //    class FakeLivingEntity<T extends EntityLiving> extends FakeEntityImpl<T> {
  //
  //      protected FakeLivingEntity(T entity) {
  //        super(entity);
  //      }
  //
  //      protected Packet<?> spawnPacket() {
  //        return new PacketPlayOutSpawnEntityLiving(entity);
  //      }
  //    }
  //
  //    class FakeArmorStand extends FakeLivingEntity<EntityArmorStand> {
  //
  //      private final ItemStack head;
  //
  //      public FakeArmorStand(World world, ItemStack head) {
  //        super(new EntityArmorStand(((CraftWorld) world).getHandle()));
  //        this.head = head;
  //
  //        entity.setInvisible(true);
  //        NBTTagCompound tag = entity.getNBTTag();
  //        if (tag == null) {
  //          tag = new NBTTagCompound();
  //        }
  //        entity.c(tag);
  //        tag.setBoolean("Silent", true);
  //        tag.setBoolean("Invulnerable", true);
  //        tag.setBoolean("NoGravity", true);
  //        tag.setBoolean("NoAI", true);
  //        entity.f(tag);
  //      }
  //
  //      @Override
  //      public void spawn(Player viewer, Location location, Vector velocity) {
  //        super.spawn(viewer, location, velocity);
  //        if (head != null) wear(viewer, 4, head);
  //      }
  //    }
  //
  //    class FakeWitherSkull extends FakeEntityImpl<EntityWitherSkull> {
  //      public FakeWitherSkull(World world) {
  //        super(new EntityWitherSkull(((CraftWorld) world).getHandle()));
  //      }
  //
  //      protected Packet<?> spawnPacket() {
  //        return new PacketPlayOutSpawnEntity(entity, 66);
  //      }
  //
  //      @Override
  //      public void wear(Player viewer, int slot, ItemStack item) {}
  //    }
  //
  //    static void setFireworksExpectedLifespan(Firework firework, int ticks) {
  //      ((CraftFirework) firework).getHandle().expectedLifespan = ticks;
  //    }
  //
  //    static void setFireworksTicksFlown(Firework firework, int ticks) {
  //      EntityFireworks entityFirework = ((CraftFirework) firework).getHandle();
  //      entityFirework.ticksFlown = ticks;
  //    }
  //
  //    static void skipFireworksLaunch(Firework firework) {
  //      setFireworksExpectedLifespan(firework, 2);
  //      setFireworksTicksFlown(firework, 2);
  //      sendEntityMetadataToViewers(firework, false);
  //    }
  //
  //    static boolean isCraftItemArrowEntity(org.bukkit.entity.Item item) {
  //      return ((CraftItem) item).getHandle() instanceof EntityArrow;
  //    }
  //
  //    static void fakePlayerItemPickup(Player player, org.bukkit.entity.Item item) {
  //      float pitch = (((float) (Math.random() - Math.random()) * 0.7F + 1.0F) * 2.0F);
  //      item.getWorld().playSound(item.getLocation(), org.bukkit.Sound.ITEM_PICKUP, 0.2F, pitch);
  //
  //      NMSHacks.sendPacketToViewers(
  //          item, new PacketPlayOutCollect(item.getEntityId(), player.getEntityId()));
  //
  //      item.remove();
  //    }
}
