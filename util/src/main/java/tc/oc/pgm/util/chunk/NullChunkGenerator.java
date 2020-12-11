package tc.oc.pgm.util.chunk;

import java.util.Random;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

/** A chunk generator that creates empty chunks. */
public class NullChunkGenerator extends ChunkGenerator {
  public static final NullChunkGenerator INSTANCE = new NullChunkGenerator();
  private static final byte[] CHUNK = new byte[0];

  private NullChunkGenerator() {}

  @Override
  public byte[] generate(final World world, final Random random, final int x, final int z) {
    return CHUNK;
  }
}
