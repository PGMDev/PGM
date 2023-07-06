package tc.oc.pgm.platform.v1_13;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

public class NullChunkGenerator extends ChunkGenerator {
  @Override
  public List<BlockPopulator> getDefaultPopulators(World world) {
    return Collections.emptyList();
  }

  @SuppressWarnings("deprecation")
  @Override
  public ChunkData generateChunkData(
      World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
    ChunkData chunkData = super.createChunkData(world);

    // For everyblock in the chunk set the biome to plains
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        biome.setBiome(x, z, Biome.PLAINS);
      }
    }

    return chunkData;
  }

  @Override
  public boolean canSpawn(World world, int x, int z) {
    return true;
  }

  @Override
  public Location getFixedSpawnLocation(World world, Random random) {
    return new Location(world, 0, 128, 0);
  }
}
