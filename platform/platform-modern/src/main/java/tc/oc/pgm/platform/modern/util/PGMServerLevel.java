package tc.oc.pgm.platform.modern.util;

import io.papermc.paper.world.PaperWorldLoader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapIndex;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.PGM;

@NullMarked
public class PGMServerLevel extends ServerLevel {
  public PGMServerLevel(
      MinecraftServer server,
      Executor dispatcher,
      LevelStorageSource.LevelStorageAccess levelStorageAccess,
      WorldGenSettings worldGenSettings,
      ResourceKey<Level> dimension,
      LevelStem levelStem,
      boolean isDebug,
      long biomeZoomSeed,
      List<CustomSpawner> customSpawners,
      boolean tickTime,
      ResourceKey<LevelStem> typeKey,
      World.Environment env,
      ChunkGenerator gen,
      BiomeProvider biomeProvider,
      SavedDataStorage savedDataStorage,
      PaperWorldLoader.LoadedWorldData loadedWorldData) {
    super(
        server,
        dispatcher,
        levelStorageAccess,
        worldGenSettings,
        dimension,
        levelStem,
        isDebug,
        biomeZoomSeed,
        customSpawners,
        tickTime,
        typeKey,
        env,
        gen,
        biomeProvider,
        savedDataStorage,
        loadedWorldData);
  }

  /**
   * All map-related methods redirect to level-specific {@link getDataStorage} rather than
   * {@code this.getServer().getDataStorage()}
   */
  @Override
  public @Nullable MapItemSavedData getMapData(final MapId id) {
    // Paper start - Call missing map initialize event and set id
    final SavedDataStorage storage = getDataStorage();

    final Optional<net.minecraft.world.level.saveddata.SavedData> cacheEntry =
        storage.cache.get(MapItemSavedData.type(id));
    if (cacheEntry == null) { // Cache did not contain, try to load and may init
      final MapItemSavedData mapData =
          storage.get(MapItemSavedData.type(id)); // get populates the cache
      if (mapData != null) { // map was read, init it and return
        mapData.id = id;
        new org.bukkit.event.server.MapInitializeEvent(mapData.mapView).callEvent();
        return mapData;
      }

      return null; // Map does not exist, reading failed.
    }
    // Cache entry exists, update it with the id ref and return.
    if (cacheEntry.orElse(null) instanceof final MapItemSavedData mapItemSavedData) {
      mapItemSavedData.id = id;
      return mapItemSavedData;
    }

    return null;
    // Paper end - Call missing map initialize event and set id
  }

  @Override
  public void setMapData(final MapId id, final MapItemSavedData data) {
    // CraftBukkit start
    data.id = id;
    org.bukkit.event.server.MapInitializeEvent event =
        new org.bukkit.event.server.MapInitializeEvent(data.mapView);
    event.callEvent();
    // CraftBukkit end
    getDataStorage().set(MapItemSavedData.type(id), data);
  }

  @Override
  public MapId getFreeMapId() {
    return getDataStorage().computeIfAbsent(MapIndex.TYPE).getNextMapId();
  }

  // Allow command blocks to be disabled via config
  @Override
  public boolean isCommandBlockEnabled() {
    if (!PGM.get().getConfiguration().allowCommandBlocks()) return false;
    return super.isCommandBlockEnabled();
  }
}
