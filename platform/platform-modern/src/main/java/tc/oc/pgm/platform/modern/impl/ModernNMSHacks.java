package tc.oc.pgm.platform.modern.impl;

import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;
import static tc.oc.pgm.util.nms.Packets.ENTITIES;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import io.papermc.paper.world.PaperWorldLoader;
import io.papermc.paper.world.migration.WorldFolderMigration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.TickTask;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.generator.CraftWorldInfo;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.platform.modern.PgmBootstrap;
import tc.oc.pgm.platform.modern.material.ModernBlockMaterialData;
import tc.oc.pgm.platform.modern.util.PGMServerLevel;
import tc.oc.pgm.util.DataVersions;
import tc.oc.pgm.util.chunk.NullChunkGenerator;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.skin.Skin;

@Supports(value = PAPER, minVersion = "26.2")
public class ModernNMSHacks implements NMSHacks {
  @Override
  public void skipFireworksLaunch(Firework firework) {
    firework.setTicksToDetonate(2);
    firework.setTicksFlown(2);
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
    entity.setAcceleration(direction.multiply(0.1D));
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

    for (int i = 0; i < chunk.getSections().length; i++) {
      var section = chunk.getSections()[i];
      if (section == null || section.hasOnlyAir()) continue;

      var states = section.getStates();
      if (!states.maybeHas(bs -> bs.getBukkitMaterial() == material)) continue;

      final int chunkY = chunk.getSectionYFromSectionIndex(i) << 4;

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
      PGM.get().getLogger().log(Level.SEVERE, "Failed to create world " + worldName, t);
      return null;
    }
  }

  /** {@link org.bukkit.craftbukkit.CraftServer#createWorld} adapted to support custom dimensions */
  public World createWorld(@NonNull WorldCreator creator) {
    var server = (CraftServer) Bukkit.getServer();
    var console = server.getServer(); // NMS server

    String name = creator.name();
    ChunkGenerator chunkGenerator = creator.generator();
    BiomeProvider biomeProvider = creator.biomeProvider();
    World world = server.getWorld(name);
    NamespacedKey worldKey = new NamespacedKey("pgm", name);
    World worldByKey = server.getWorld(worldKey);
    if (world != null || worldByKey != null) {
      if (world == worldByKey) {
        return world;
      }
      throw new IllegalArgumentException("Cannot create a world with key " + worldKey + " and name "
          + name + " one (or both) already match a world that exists");
    }

    if (chunkGenerator == null) {
      chunkGenerator = server.getGenerator(name);
    }

    if (biomeProvider == null) {
      biomeProvider = server.getBiomeProvider(name);
    }

    ResourceKey<LevelStem> actualDimension =
        switch (creator.environment()) {
          case NORMAL -> LevelStem.OVERWORLD;
          case NETHER -> LevelStem.NETHER;
          case THE_END -> LevelStem.END;
          default ->
            throw new IllegalArgumentException("Illegal dimension (" + creator.environment() + ")");
        };

    LevelStem configuredStem =
        console.registryAccess().lookupOrThrow(Registries.LEVEL_STEM).getValue(actualDimension);
    if (configuredStem == null) {
      throw new IllegalStateException("Missing configured level stem " + actualDimension);
    }

    ResourceKey<net.minecraft.world.level.Level> dimensionKey =
        CraftNamespacedKey.toResourceKey(Registries.DIMENSION, worldKey);

    // PGM: We need to read data version from level.dat pre-migration
    int dataVersion = MISC_UTILS.getWorldDataVersion(
        server.getWorldContainer().toPath().resolve(name).resolve("level.dat"));
    WorldGenSettings legacyWorldGenSettings = readLegacyWorldGenSettings(console, name);

    try {
      WorldFolderMigration.migrateApiWorld(
          console.storageSource, console.registryAccess(), name, actualDimension, dimensionKey);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to migrate legacy world " + name, ex);
    }

    PaperWorldLoader.LoadedWorldData loadedWorldData =
        PaperWorldLoader.loadWorldData(console, dimensionKey, name);

    PrimaryLevelData primaryLevelData = (PrimaryLevelData) console.getWorldData();

    // PGM: only load pre-existing worlds; return null if no WorldGenSettings found
    WorldGenSettings worldGenSettings = LevelStorageSource.readExistingSavedData(
            console.storageSource, dimensionKey, console.registryAccess(), WorldGenSettings.TYPE)
        .result()
        .orElse(legacyWorldGenSettings);
    if (worldGenSettings == null) return null;

    // PGM: override seed from WorldCreator
    worldGenSettings = new WorldGenSettings(
        worldGenSettings.options().withSeed(OptionalLong.of(creator.seed())),
        worldGenSettings.dimensions());

    WorldLoader.DataLoadContext context = console.worldLoaderContext;
    RegistryAccess.Frozen registryAccess = context.datapackDimensions();
    Registry<LevelStem> contextLevelStemRegistry =
        registryAccess.lookupOrThrow(Registries.LEVEL_STEM);

    long biomeZoomSeed = BiomeManager.obfuscateSeed(worldGenSettings.options().seed());

    LevelStem customStem = worldGenSettings.dimensions().get(actualDimension).orElse(null);
    if (customStem == null) customStem = contextLevelStemRegistry.getValue(actualDimension);
    if (customStem == null) {
      throw new IllegalStateException(
          "Missing level stem for world " + name + " using key " + actualDimension);
    }

    // PGM: replace dimension type for legacy worlds
    if (dataVersion < DataVersions.V1_18_EXP_1 && actualDimension == LevelStem.OVERWORLD) {
      var dimHolder = console
          .registryAccess()
          .lookupOrThrow(Registries.DIMENSION_TYPE)
          .getOrThrow(PgmBootstrap.LEGACY_OVERWORLD);
      customStem = new LevelStem(dimHolder, customStem.generator());
    }

    WorldInfo worldInfo = new CraftWorldInfo(
        loadedWorldData.bukkitName(),
        CraftNamespacedKey.fromMinecraft(dimensionKey.identifier()),
        worldGenSettings.options().seed(),
        primaryLevelData.enabledFeatures(),
        creator.environment(),
        customStem.type().value(),
        customStem.generator(),
        console.registryAccess(),
        loadedWorldData.uuid());
    if (biomeProvider == null && chunkGenerator != null) {
      biomeProvider = chunkGenerator.getDefaultBiomeProvider(worldInfo);
    }

    SavedDataStorage savedDataStorage = new SavedDataStorage(
        console.storageSource.getDimensionPath(dimensionKey).resolve(LevelResource.DATA.id()),
        console.getFixerUpper(),
        console.registryAccess());
    savedDataStorage.set(WorldGenSettings.TYPE, worldGenSettings);

    ServerLevel serverLevel = new PGMServerLevel(
        console,
        Util.backgroundExecutor(),
        console.storageSource,
        worldGenSettings,
        dimensionKey,
        customStem,
        false, // isDebug
        biomeZoomSeed,
        ImmutableList.of(), // no spawners
        true, // tickTime
        actualDimension,
        creator.environment(),
        chunkGenerator,
        biomeProvider,
        savedDataStorage,
        loadedWorldData);

    if (server.getWorld(name) == null) {
      return null;
    }

    if (dataVersion >= DataVersions.V1_13)
      serverLevel
          .getWorld()
          .setMetadata("is-post-flattening", new FixedMetadataValue(PGM.get(), true));

    console.addLevel(serverLevel);
    console.initWorld(serverLevel, creator);
    serverLevel.setSpawnSettings(true);
    console.prepareLevel(serverLevel);
    return serverLevel.getWorld();
  }

  private static @Nullable WorldGenSettings readLegacyWorldGenSettings(
      DedicatedServer console, String name) {
    try (LevelStorageSource.LevelStorageAccess sourceAccess =
        console.storageSource.parent().createAccess(name)) {
      if (!sourceAccess.hasWorldData()) return null;

      Dynamic<?> levelData = sourceAccess.getUnfixedDataTagWithFallback();
      Dynamic<?> fixedLevelData =
          DataFixers.getFileFixer().fix(sourceAccess, levelData, new UpgradeProgress());
      Dynamic<?> registryLevelData =
          RegistryOps.injectRegistryContext(fixedLevelData, console.registryAccess());

      return registryLevelData
          .get("WorldGenSettings")
          .read(WorldGenSettings.CODEC)
          .result()
          .orElse(null);
    } catch (IOException | RuntimeException ex) {
      PGM.get()
          .getLogger()
          .log(Level.WARNING, "Failed to read legacy world gen settings for " + name, ex);
      return null;
    }
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
    player.setKiller(null);
  }

  @Override
  public double getTPS() {
    return Bukkit.getServer().getTPS()[0];
  }

  @Override
  public void postToMainThread(Plugin plugin, boolean priority, Runnable task) {
    DedicatedServer server = ((CraftServer) Bukkit.getServer()).getServer();
    server.schedule(new TickTask(server.getTickCount(), () -> {
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
  public int allocateEntityId(World world) {
    return Bukkit.getUnsafe().nextEntityId(world);
  }
}
