package tc.oc.pgm.util.material;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Iterator;
import org.bukkit.block.BlockState;

/** Efficiently counts distinct {@link MaterialData}s */
public class MaterialCounter {
  private final Int2IntMap counts;

  public MaterialCounter() {
    this.counts = new Int2IntOpenHashMap();
  }

  public boolean contains(int encodedMaterial) {
    return counts.containsKey(encodedMaterial);
  }

  public boolean contains(BlockMaterialData material) {
    return contains(material.encoded());
  }

  public int get(int encodedMaterial) {
    return counts.get(encodedMaterial);
  }

  public int get(BlockMaterialData material) {
    return get(material.encoded());
  }

  public int increment(int encodedMaterial, int count) {
    return counts.merge(encodedMaterial, count, (key, v) -> v + count);
  }

  public int increment(BlockState block, int count) {
    return increment(MaterialData.block(block).encoded(), count);
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
