package tc.oc.pgm.platform.modern.util;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import io.papermc.paper.world.PaperWorldLoader;
import io.papermc.paper.world.migration.WorldFolderMigration;
import java.io.IOException;
import java.util.OptionalLong;
import java.util.logging.Level;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.generator.CraftWorldInfo;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.metadata.FixedMetadataValue;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.DataVersions;
import tc.oc.pgm.util.chunk.NullChunkGenerator;
import tc.oc.pgm.util.world.WorldCreationInfo;
import tc.oc.pgm.util.world.WorldFormat;

public abstract class ModernWorldLoader {

  /** @return the loaded world, or null if it could not be created */
  public static @Nullable World createWorld(WorldCreationInfo info) {
    boolean terrain = info.terrain();
    try {
      return createWorld(
          new WorldCreator(info.name())
              .environment(info.environment())
              .generator(terrain ? null : NullChunkGenerator.INSTANCE)
              .seed(terrain ? info.seed() : 0),
          info.format(),
          info.dataVersion());
    } catch (Throwable t) {
      PGM.get().getLogger().log(Level.SEVERE, "Failed to create world " + info.name(), t);
      return null;
    }
  }

  /**
   * Loads match worlds, in either {@link WorldFormat}, into their own PGM-namespaced dimension.
   *
   * <p>Bukkit's own world creation assumes a CraftBukkit world folder in the world container, so
   * this is {@link org.bukkit.craftbukkit.CraftServer#createWorld} adapted for PGM's needs.
   */
  private static @Nullable World createWorld(
      @NonNull WorldCreator creator, WorldFormat format, int dataVersion) {
    var server = (CraftServer) Bukkit.getServer();
    var console = server.getServer(); // NMS server

    String name = creator.name();
    ChunkGenerator chunkGenerator = creator.generator();
    BiomeProvider biomeProvider = creator.biomeProvider();
    World world = server.getWorld(name);
    // PGM: set our own world key with our own namespace
    NamespacedKey worldKey = PgmKeys.worldKey(name);
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

    RegistryAccess registryAccess = console.registryAccess();
    // PGM: set our own dimension key with our own namespace
    ResourceKey<net.minecraft.world.level.Level> dimensionKey = PgmKeys.dimensionKey(name);
    Registry<LevelStem> levelStemRegistry = registryAccess.lookupOrThrow(Registries.LEVEL_STEM);
    LevelStem configuredStem = levelStemRegistry.getValue(actualDimension);
    if (configuredStem == null) {
      throw new IllegalStateException("Missing configured level stem " + actualDimension);
    }

    // PGM: bring the world folder into a loadable state before reading anything back out of it
    WorldGenSettings legacyWorldGenSettings =
        prepareWorld(server, name, format, actualDimension, dimensionKey);

    PaperWorldLoader.LoadedWorldData loadedWorldData =
        PaperWorldLoader.loadWorldData(console, dimensionKey, name);
    PrimaryLevelData primaryLevelData = (PrimaryLevelData) console.getWorldData();

    // PGM: only load pre-existing worlds; return null if no WorldGenSettings found
    WorldGenSettings worldGenSettings = LevelStorageSource.readExistingSavedData(
            console.storageSource, dimensionKey, registryAccess, WorldGenSettings.TYPE)
        .result()
        .orElse(legacyWorldGenSettings);
    if (worldGenSettings == null) {
      PGM.get().getLogger().severe("World " + name + " has no readable world gen settings");
      return null;
    }

    // PGM: override seed from WorldCreator
    worldGenSettings = new WorldGenSettings(
        worldGenSettings.options().withSeed(OptionalLong.of(creator.seed())),
        worldGenSettings.dimensions());

    long biomeZoomSeed = BiomeManager.obfuscateSeed(worldGenSettings.options().seed());

    LevelStem customStem = worldGenSettings.dimensions().get(actualDimension).orElse(null);
    if (customStem == null) customStem = levelStemRegistry.getValue(actualDimension);
    if (customStem == null) {
      throw new IllegalStateException(
          "Missing level stem for world " + name + " using key " + actualDimension);
    }

    // PGM: replace dimension type for legacy worlds
    if (needsLegacyOverworld(format, dataVersion) && actualDimension == LevelStem.OVERWORLD) {
      var dimHolder = registryAccess
          .lookupOrThrow(Registries.DIMENSION_TYPE)
          .getOrThrow(PgmKeys.LEGACY_OVERWORLD);
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
        registryAccess,
        loadedWorldData.uuid());
    if (biomeProvider == null && chunkGenerator != null) {
      biomeProvider = chunkGenerator.getDefaultBiomeProvider(worldInfo);
    }

    SavedDataStorage savedDataStorage = new SavedDataStorage(
        console.storageSource.getDimensionPath(dimensionKey).resolve(LevelResource.DATA.id()),
        console.getFixerUpper(),
        registryAccess);
    savedDataStorage.set(WorldGenSettings.TYPE, worldGenSettings);

    // PGM: pass PGMServerLevel so we can fix displayed maps and disable command blocks
    ServerLevel serverLevel = new PGMServerLevel(
        console,
        Util.backgroundExecutor(),
        console.storageSource,
        worldGenSettings,
        dimensionKey,
        customStem,
        false, // PGM: force isDebug false
        biomeZoomSeed,
        ImmutableList.of(), // PGM: no spawners
        true, // PGM: force tickTime true
        actualDimension,
        creator.environment(),
        chunkGenerator,
        biomeProvider,
        savedDataStorage,
        loadedWorldData);

    // PGM: equivalent to worlds.containsKey(name.toLowerCase) in CraftServer
    if (server.getWorld(name) == null) {
      return null;
    }

    // PGM: add metadata if map is post-flattening for WorldProblemListener
    if (isPostFlattening(format, dataVersion))
      serverLevel
          .getWorld()
          .setMetadata("is-post-flattening", new FixedMetadataValue(PGM.get(), true));

    console.addLevel(serverLevel);
    console.initWorld(serverLevel, creator);
    serverLevel.setSpawnSettings(true);
    console.prepareLevel(serverLevel);
    return serverLevel.getWorld();
  }

  private static boolean isPostFlattening(WorldFormat format, int dataVersion) {
    return format == WorldFormat.DIMENSION || dataVersion >= DataVersions.V1_13;
  }

  private static boolean needsLegacyOverworld(WorldFormat format, int dataVersion) {
    return format == WorldFormat.LEGACY && dataVersion < DataVersions.V1_18_EXP_1;
  }

  /**
   * Migrate a legacy world folder into the per-dimension layout, so it can be read back out.
   *
   * @return world gen settings recovered from the pre-migration {@code level.dat}, if any
   */
  private static @Nullable WorldGenSettings prepareWorld(
      CraftServer server,
      String name,
      WorldFormat format,
      ResourceKey<LevelStem> stem,
      ResourceKey<net.minecraft.world.level.Level> dimensionKey) {
    // We don't need to prepare worlds that are in the dimension format already
    if (format == WorldFormat.DIMENSION) return null;

    var console = server.getServer();
    var legacyWorldGenSettings = readLegacyWorldGenSettings(console, name);

    try {
      WorldFolderMigration.migrateApiWorld(
          console.storageSource, console.registryAccess(), name, stem, dimensionKey);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to migrate legacy world " + name, ex);
    }

    return legacyWorldGenSettings;
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
}
