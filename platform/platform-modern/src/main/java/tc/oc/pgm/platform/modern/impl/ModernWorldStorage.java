package tc.oc.pgm.platform.modern.impl;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
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
import tc.oc.pgm.util.world.WorldStorage;

@Supports(value = PAPER, minVersion = "26.1.2")
public class ModernWorldStorage implements WorldStorage {
  /** Constant from LevelStorageSource, sounds way too high (104mb) but better than unbounded */
  public static final long MAX_HEAP = 104857600L;

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
  public List<File> getWorldDirectories() {
    // Probe a non-existent dimension to get the dimension root folder
    var dimensionRoot = getDimensionPath(DUMMY_WORLD).getParent().toFile();
    return List.of(dimensionRoot, Bukkit.getServer().getWorldContainer());
  }

  @Override
  public int getWorldDataVersion(Path worldDir) {
    try {
      var root = NbtIo.readCompressed(worldDir.resolve("level.dat"), NbtAccounter.create(MAX_HEAP));
      return NbtUtils.getDataVersion(root.getCompoundOrEmpty("Data"), DataVersions.LEGACY);
    } catch (Throwable ignored) {
      // In case we cannot read the level.dat file, return a constant
      return DataVersions.LEGACY;
    }
  }
}
