package tc.oc.pgm.platform.modern.util;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapIndex;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.Nullable;

public class PGMServerLevel extends ServerLevel {
  public PGMServerLevel(
      MinecraftServer server,
      Executor dispatcher,
      LevelStorageSource.LevelStorageAccess levelStorageAccess,
      PrimaryLevelData serverLevelData,
      ResourceKey<Level> dimension,
      LevelStem levelStem,
      ChunkProgressListener progressListener,
      boolean isDebug,
      long biomeZoomSeed,
      List<CustomSpawner> customSpawners,
      boolean tickTime,
      @Nullable RandomSequences randomSequences,
      World.Environment env,
      ChunkGenerator gen,
      BiomeProvider biomeProvider) {
    super(
        server,
        dispatcher,
        levelStorageAccess,
        serverLevelData,
        dimension,
        levelStem,
        progressListener,
        isDebug,
        biomeZoomSeed,
        customSpawners,
        tickTime,
        randomSequences,
        env,
        gen,
        biomeProvider);
  }

  // Redirect all map operations to world-level storage
  @Nullable
  @Override
  public MapItemSavedData getMapData(MapId mapId) {
    // Paper start - Call missing map initialize event and set id
    final DimensionDataStorage storage = getDataStorage();

    final Optional<SavedData> cacheEntry = storage.cache.get(MapItemSavedData.type(mapId));
    if (cacheEntry == null) { // Cache did not contain, try to load and may init
      final MapItemSavedData mapData =
          storage.get(MapItemSavedData.type(mapId)); // get populates the cache
      if (mapData != null) { // map was read, init it and return
        mapData.id = mapId;
        new org.bukkit.event.server.MapInitializeEvent(mapData.mapView).callEvent();
        return mapData;
      }

      return null; // Map does not exist, reading failed.
    }
    // Cache entry exists, update it with the id ref and return.
    if (cacheEntry.orElse(null) instanceof final MapItemSavedData mapItemSavedData) {
      mapItemSavedData.id = mapId;
      return mapItemSavedData;
    }

    return null;
    // Paper end - Call missing map initialize event and set id
  }

  @Override
  public void setMapData(MapId mapId, MapItemSavedData data) {
    // CraftBukkit start
    data.id = mapId;
    org.bukkit.event.server.MapInitializeEvent event =
        new org.bukkit.event.server.MapInitializeEvent(data.mapView);
    event.callEvent();
    // CraftBukkit end
    getDataStorage().set(MapItemSavedData.type(mapId), data);
  }

  @Override
  public MapId getFreeMapId() {
    return getDataStorage().computeIfAbsent(MapIndex.TYPE).getNextMapId();
  }
}
