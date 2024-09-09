package tc.oc.pgm.platform.modern.impl;

import static tc.oc.pgm.util.nms.Packets.ENTITIES;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.validation.ContentValidationException;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftFirework;
import org.bukkit.craftbukkit.generator.CraftWorldInfo;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import tc.oc.pgm.platform.modern.PgmBootstrap;
import tc.oc.pgm.platform.modern.material.ModernBlockMaterialData;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.chunk.NullChunkGenerator;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.skin.Skin;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernNMSHacks implements NMSHacks {
  @Override
  public void skipFireworksLaunch(Firework firework) {
    FireworkRocketEntity entityFirework = ((CraftFirework) firework).getHandle();
    entityFirework.lifetime = 2;
    entityFirework.life = 2;
    ENTITIES
        .entityMetadataPacket(firework.getEntityId(), firework, false)
        .sendToViewers(firework, false);
  }

  @Override
  public boolean isCraftItemArrowEntity(PlayerPickupItemEvent event) {
    return event instanceof PlayerPickupArrowEvent;
  }

  @Override
  public void freezeEntity(Entity entity) {
    if (((CraftEntity) entity).getHandle() instanceof Mob mob) {
      mob.setNoAi(true);
      mob.setNoGravity(true);
    }
  }

  @Override
  public void setFireballDirection(Fireball entity, Vector direction) {
    entity.setPower(direction.multiply(0.1D));
  }

  @Override
  public long getMonotonicTime(World world) {
    return ((CraftWorld) world).getHandle().getGameTime();
  }

  @Override
  public void resumeServer() {
    // no-op, server pausing is sportpaper-specific
  }

  @Override
  public Inventory createFakeInventory(Player viewer, Inventory realInventory) {
    Component customName;
    if (realInventory instanceof Nameable n && (customName = n.customName()) != null) {
      return realInventory instanceof DoubleChestInventory
          ? Bukkit.createInventory(viewer, realInventory.getSize(), customName)
          : Bukkit.createInventory(viewer, realInventory.getType(), customName);
    } else {
      return realInventory instanceof DoubleChestInventory
          ? Bukkit.createInventory(viewer, realInventory.getSize())
          : Bukkit.createInventory(viewer, realInventory.getType());
    }
  }

  @Override
  public List<Block> getBlocks(Chunk bukkitChunk, Material material) {
    CraftChunk craftChunk = (CraftChunk) bukkitChunk;
    List<Block> blocks = new ArrayList<>();

    var nmsBlock = CraftMagicNumbers.getBlock(material);
    var chunk = craftChunk.getHandle(ChunkStatus.FULL);

    int baseY = chunk.getMinBuildHeight();
    for (int i = 0; i < chunk.getSections().length; i++) {
      var section = chunk.getSections()[i];
      if (section == null || section.hasOnlyAir()) continue;

      var states = section.getStates();
      if (!states.maybeHas(bs -> bs.getBukkitMaterial() == material)) continue;

      final int chunkY = baseY + (i * LevelChunkSection.SECTION_HEIGHT);

      // Iteration order is relevant, as indexes are packed as x | z << 4 | y << 8
      for (int y = 0; y < 16; y++) {
        for (int z = 0; z < 16; z++) {
          for (int x = 0; x < 16; x++) {
            if (states.get(x, y, z).getBlock() == nmsBlock)
              blocks.add(bukkitChunk.getBlock(x, chunkY + y, z));
          }
        }
      }
    }

    return blocks;
  }

  @Override
  public void setSkullMetaOwner(SkullMeta meta, String name, UUID uuid, Skin skin) {
    var profile = Bukkit.createProfile(uuid, name);
    profile.setProperty(new ProfileProperty("textures", skin.getData(), skin.getSignature()));
    meta.setPlayerProfile(profile);
  }

  @Override
  public World createWorld(String worldName, World.Environment env, boolean terrain, long seed) {
    try {
      return createWorld(new WorldCreator(worldName)
          .environment(env)
          .generator(terrain ? null : NullChunkGenerator.INSTANCE)
          .seed(terrain ? seed : 0));
    } catch (Throwable t) {
      BukkitUtils.getPlugin()
          .getLogger()
          .log(Level.SEVERE, "Failed to create world " + worldName, t);
      return null;
    }
  }

  /** This is ripped straight out of craft bukkit, modified to support custom dimensions */
  public World createWorld(WorldCreator creator) {
    var server = (CraftServer) Bukkit.getServer();
    var console = server.getServer(); // NMS server

    Preconditions.checkArgument(creator != null, "WorldCreator cannot be null");
    String name = creator.name();
    ChunkGenerator generator = creator.generator();
    BiomeProvider biomeProvider = creator.biomeProvider();
    File folder = new File(server.getWorldContainer(), name);
    World world = server.getWorld(name);

    // Paper start
    World worldByKey = server.getWorld(creator.key());
    if (world != null || worldByKey != null) {
      if (world == worldByKey) {
        return world;
      }
      throw new IllegalArgumentException("Cannot create a world with key " + creator.key()
          + " and name " + name + " one (or both) already match a world that exists");
    }
    // Paper end
    if (folder.exists()) {
      Preconditions.checkArgument(
          folder.isDirectory(), "File (%s) exists and isn't a folder", name);
    }

    if (generator == null) {
      generator = server.getGenerator(name);
    }
    if (biomeProvider == null) {
      biomeProvider = server.getBiomeProvider(name);
    }

    ResourceKey<LevelStem> actualDimension =
        switch (creator.environment()) {
          case NORMAL -> LevelStem.OVERWORLD;
          case NETHER -> LevelStem.NETHER;
          case THE_END -> LevelStem.END;
          default -> throw new IllegalArgumentException(
              "Illegal dimension (" + creator.environment() + ")");
        };

    LevelStorageSource.LevelStorageAccess worldSession;
    try {
      worldSession = LevelStorageSource.createDefault(server.getWorldContainer().toPath())
          .validateAndCreateAccess(name, actualDimension);
    } catch (IOException | ContentValidationException ex) {
      throw new RuntimeException(ex);
    }

    if (!worldSession.hasWorldData()) {
      throw new UnsupportedOperationException("Cannot use PGM createWorld with an empty level.dat");
    }

    Dynamic<?> dynamic;
    net.minecraft.world.level.storage.LevelSummary worldinfo;
    try {
      dynamic = worldSession.getDataTag();
      worldinfo = worldSession.getSummary(dynamic);
    } catch (NbtException | ReportedNbtException | IOException ioexception) {
      LevelStorageSource.LevelDirectory convertable_b = worldSession.getLevelDirectory();

      MinecraftServer.LOGGER.warn(
          "Failed to load world data from {}", convertable_b.dataFile(), ioexception);
      return null;
    }

    if (worldinfo.requiresManualConversion()) {
      MinecraftServer.LOGGER.info(
          "This world must be opened in an older version (like 1.6.4) to be safely converted");
      return null;
    }

    if (!worldinfo.isCompatible()) {
      MinecraftServer.LOGGER.info("This world was created by an incompatible version.");
      return null;
    }

    PrimaryLevelData worlddata;
    WorldLoader.DataLoadContext worldloader_a = console.worldLoader;
    RegistryAccess.Frozen iregistrycustom_dimension = worldloader_a.datapackDimensions();
    net.minecraft.core.Registry<LevelStem> iregistry =
        iregistrycustom_dimension.registryOrThrow(Registries.LEVEL_STEM);
    LevelDataAndDimensions leveldataanddimensions = LevelStorageSource.getLevelDataAndDimensions(
        dynamic, worldloader_a.dataConfiguration(), iregistry, worldloader_a.datapackWorldgen());
    worlddata = (PrimaryLevelData) leveldataanddimensions.worldData();

    iregistrycustom_dimension = leveldataanddimensions.dimensions().dimensionsRegistryAccess();
    iregistry = iregistrycustom_dimension.registryOrThrow(Registries.LEVEL_STEM);

    worlddata.customDimensions = iregistry;
    worlddata.checkName(name);
    worlddata.setModdedInfo(
        console.getServerModName(), console.getModdedStatus().shouldReportAsModified());

    long j =
        BiomeManager.obfuscateSeed(worlddata.worldGenOptions().seed()); // Paper - use world seed

    List<CustomSpawner> list = List.of();
    LevelStem worlddimension = iregistry.get(actualDimension);

    WorldInfo worldInfo = new CraftWorldInfo(
        worlddata,
        worldSession,
        creator.environment(),
        worlddimension.type().value(),
        worlddimension.generator(),
        console.registryAccess()); // Paper - Expose vanilla BiomeProvider from WorldInfo
    if (biomeProvider == null && generator != null) {
      biomeProvider = generator.getDefaultBiomeProvider(worldInfo);
    }

    // If the world is 1.17 or older, replace dimension type
    boolean isOld = worldinfo.levelVersion().minecraftVersion().getVersion() <= 2730;
    if (isOld && actualDimension == LevelStem.OVERWORLD) {
      var dimReg = console.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
      var dimHolder = dimReg.getHolderOrThrow(PgmBootstrap.LEGACY_OVERWORLD);

      worlddimension = new LevelStem(dimHolder, worlddimension.generator());
    }
    ResourceKey<net.minecraft.world.level.Level> worldKey;
    worldKey = ResourceKey.create(
        Registries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath(
            creator.key().namespace(), creator.key().value()));

    worlddata.getGameRules().getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0, null);

    ServerLevel internal = new ServerLevel(
        console,
        console.executor,
        worldSession,
        worlddata,
        worldKey,
        Objects.requireNonNull(worlddimension),
        console.progressListenerFactory.create(
            worlddata.getGameRules().getInt(GameRules.RULE_SPAWN_CHUNK_RADIUS)),
        false, // isDebug
        j,
        ImmutableList.of(),
        true, // tickTime
        console.overworld().getRandomSequences(),
        creator.environment(),
        generator,
        biomeProvider);

    if (server.getWorld(name) == null) {
      return null;
    }

    console.addLevel(internal);
    console.initWorld(internal, worlddata, worlddata, worlddata.worldGenOptions());
    internal.setSpawnSettings(true, true);
    console.prepareLevels(internal.getChunkSource().chunkMap.progressListener, internal);
    server.getPluginManager().callEvent(new WorldLoadEvent(internal.getWorld()));
    return internal.getWorld();
  }

  @Override
  public boolean canMineBlock(BlockMaterialData blockMaterial, Player player) {
    return ((ModernBlockMaterialData) blockMaterial)
        .getBlock()
        .isPreferredTool(player.getInventory().getItemInMainHand());
  }

  @Override
  public void resetDimension(World world) {
    // no-op
  }

  @Override
  public void cleanupWorld(World world) {
    // no-op
  }

  @Override
  public void cleanupPlayer(Player player) {
    // no-op
  }

  @Override
  public double getTPS() {
    return Bukkit.getServer().getTPS()[0];
  }

  @Override
  public void postToMainThread(Plugin plugin, boolean priority, Runnable task) {
    DedicatedServer server = ((CraftServer) Bukkit.getServer()).getServer();
    server.tell(new TickTask(server.getTickCount(), () -> {
      try {
        task.run();
      } catch (Throwable t) {
        plugin
            .getLogger()
            .log(Level.SEVERE, "Exception running task from plugin " + plugin.getName(), t);
      }
    }));
  }

  @Override
  public int getMaxWorldSize(World world) {
    return ((CraftWorld) world).getHandle().getWorldBorder().getAbsoluteMaxSize();
  }

  @Override
  public int allocateEntityId() {
    return Bukkit.getUnsafe().nextEntityId();
  }
}
