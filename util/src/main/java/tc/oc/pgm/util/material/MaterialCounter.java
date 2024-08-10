package tc.oc.pgm.util.material;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Iterator;
import org.bukkit.block.BlockState;

/** Efficiently counts distinct {@link BlockMaterialData}s */
public class MaterialCounter {
  private final Int2IntMap counts;

  public MaterialCounter() {
    this.counts = new Int2IntOpenHashMap();
  }

  public boolean contains(BlockMaterialData material) {
    return counts.containsKey(material.encoded());
  }

  public int get(BlockMaterialData material) {
    return counts.get(material.encoded());
  }

  public void increment(BlockState block, int count) {
    counts.merge(MaterialData.block(block).encoded(), count, (key, v) -> v + count);
  }

  public void clear() {
    counts.clear();
  }

  public Iterable<BlockMaterialData> materials() {
    return () -> new Iterator<>() {
      final IntIterator iter = counts.keySet().iterator();

      @Override
      public boolean hasNext() {
        return iter.hasNext();
      }

      @Override
      public BlockMaterialData next() {
        return MaterialData.decode(iter.nextInt());
      }

      @Override
      public void remove() {
        iter.remove();
      }
    };
  }
}
