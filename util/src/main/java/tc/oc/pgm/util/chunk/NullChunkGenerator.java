package tc.oc.pgm.util.chunk;

import java.util.Random;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

public class NullChunkGenerator extends ChunkGenerator {
  @Override
  public byte[] generate(World world, Random random, int x, int z) {
    return new byte[16 * 16 * 256];
  }
}
