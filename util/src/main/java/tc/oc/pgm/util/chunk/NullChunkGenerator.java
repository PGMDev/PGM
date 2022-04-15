package tc.oc.pgm.util.chunk;

import org.bukkit.generator.ChunkGenerator;

/** A chunk generator that creates empty chunks. */
public class NullChunkGenerator extends ChunkGenerator {
  public static final NullChunkGenerator INSTANCE = new NullChunkGenerator();

  private NullChunkGenerator() {}
}
