package tc.oc.pgm.platform.sportpaper.impl;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.server.v1_8_R3.IDataManager;
import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.ServerNBTManager;
import net.minecraft.server.v1_8_R3.WorldData;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import tc.oc.pgm.util.DataVersions;
import tc.oc.pgm.util.chunk.NullChunkGenerator;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.world.WorldCreationInfo;
import tc.oc.pgm.util.world.WorldFormat;
import tc.oc.pgm.util.world.WorldStorage;

@Supports(SPORTPAPER)
public class SpWorldStorage implements WorldStorage {
  private static final byte NBT_TAG_ANY_NUMERIC = 99;

  @Override
  public World createWorld(WorldCreationInfo info) {
    String worldName = info.name();
    boolean terrain = info.terrain();
    WorldCreator creator = new WorldCreator(worldName);

    IDataManager sdm =
        new ServerNBTManager(Bukkit.getServer().getWorldContainer(), worldName, true);
    WorldData worldData = sdm.getWorldData();
    if (worldData != null) {
      creator
          .generateStructures(worldData.shouldGenerateMapFeatures())
          .generatorSettings(worldData.getGeneratorOptions())
          .seed(worldData.getSeed())
          .type(org.bukkit.WorldType.getByName(worldData.getType().name()));
    }

    return Bukkit.getServer()
        .createWorld(creator
            .environment(info.environment())
            .generator(terrain ? null : NullChunkGenerator.INSTANCE)
            .seed(terrain ? info.seed() : creator.seed()));
  }

  @Override
  public File getWorldDirectory(String worldName, WorldFormat format) {
    return new File(Bukkit.getServer().getWorldContainer(), worldName);
  }

  @Override
  public List<File> getWorldDirectories() {
    return List.of(Bukkit.getServer().getWorldContainer());
  }

  @Override
  public int getWorldDataVersion(Path worldDir, WorldFormat format) {
    var levelDat = worldDir.resolve(WorldFormat.LEVEL_DAT);
    try (var in = Files.newInputStream(levelDat)) {
      var dataTag = NBTCompressedStreamTools.a(in).getCompound("Data");
      return dataTag.hasKeyOfType("DataVersion", NBT_TAG_ANY_NUMERIC)
          ? dataTag.getInt("DataVersion")
          : DataVersions.LEGACY;
    } catch (Throwable ignored) {
      // In case we cannot read the level.dat file, return a constant
      return DataVersions.LEGACY;
    }
  }

  @Override
  public List<String> getDiscardedFiles(WorldFormat format) {
    // Legacy maps do not often have more than the minimum necessary files
    return List.of();
  }
}
