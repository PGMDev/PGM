package tc.oc.pgm.platform.modern.impl;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.platform.modern.util.ModernWorldLoader;
import tc.oc.pgm.platform.modern.util.PgmKeys;
import tc.oc.pgm.util.DataVersions;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.world.WorldCreationInfo;
import tc.oc.pgm.util.world.WorldFormat;
import tc.oc.pgm.util.world.WorldStorage;

@Supports(value = PAPER, minVersion = "26.2")
public class ModernWorldStorage implements WorldStorage {
  /** Constant from LevelStorageSource, sounds way too high (104mb) but better than unbounded */
  public static final long MAX_HEAP = 104857600L;

  /** Unneeded or unwanted per-dimension files, which we never want to inherit from a map */
  private static final List<String> DISCARDED = List.of(
      "paper-world.yml",
      "data/minecraft/chunk_tickets.dat",
      "data/minecraft/ender_dragon_fight.dat",
      "data/minecraft/raids.dat",
      "data/minecraft/scheduled_events.dat",
      "data/minecraft/wandering_trader.dat",
      "data/minecraft/world_border.dat",
      "data/paper/metadata.dat",
      "data/paper/persistent_data_container.dat");

  /** Only ever used to resolve the dimension root */
  private static final String DUMMY_WORLD = "dummy";

  @Override
  public @Nullable World createWorld(WorldCreationInfo info) {
    return ModernWorldLoader.createWorld(info);
  }

  private static Path getDimensionPath(String worldName) {
    var console = ((CraftServer) Bukkit.getServer()).getServer();
    return console.storageSource.getDimensionPath(PgmKeys.dimensionKey(worldName));
  }

  @Override
  public File getWorldDirectory(String worldName, WorldFormat format) {
    return switch (format) {
      case DIMENSION -> getDimensionPath(worldName).toFile();
      case LEGACY -> new File(Bukkit.getServer().getWorldContainer(), worldName);
    };
  }

  @Override
  public List<File> getWorldDirectories() {
    // Probe a non-existent dimension to get the dimension root folder
    var dimensionRoot = getDimensionPath(DUMMY_WORLD).getParent().toFile();
    return List.of(dimensionRoot, Bukkit.getServer().getWorldContainer());
  }

  @Override
  public int getWorldDataVersion(Path worldDir, WorldFormat format) {
    try {
      return switch (format) {
        case DIMENSION ->
          NbtUtils.getDataVersion(
              readCompressed(worldDir.resolve(WorldFormat.WORLD_GEN_SETTINGS)),
              DataVersions.LEGACY);
        case LEGACY ->
          NbtUtils.getDataVersion(
              readCompressed(worldDir.resolve(WorldFormat.LEVEL_DAT)).getCompoundOrEmpty("Data"),
              DataVersions.LEGACY);
      };
    } catch (Throwable ignored) {
      // If we cannot read the world's data, assume the oldest version we support
      return DataVersions.LEGACY;
    }
  }

  private static CompoundTag readCompressed(Path file) throws IOException {
    return NbtIo.readCompressed(file, NbtAccounter.create(MAX_HEAP));
  }

  @Override
  public List<String> getDiscardedFiles(WorldFormat format) {
    return format == WorldFormat.DIMENSION ? DISCARDED : List.of();
  }
}
