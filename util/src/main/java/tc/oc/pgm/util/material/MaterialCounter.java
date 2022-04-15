package tc.oc.pgm.util.material;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.Iterator;
import org.bukkit.Material;
import org.bukkit.block.BlockState;

/// ** Efficiently counts distinct {@link MaterialData}s */
public class MaterialCounter {
  private final TObjectIntHashMap<Material> counts;

  private MaterialCounter(TObjectIntHashMap<Material> counts) {
    this.counts = counts;
  }

  public MaterialCounter() {
    this(new TObjectIntHashMap<>());
  }

  public int get(Material material) {
    return counts.get(material);
  }

  public int increment(Material material, int count) {
    return counts.adjustOrPutValue(material, count, count);
  }

  public int increment(BlockState block, int count) {
    return increment(block.getType(), count);
  }

  public void clear() {
    counts.clear();
  }

  public void addAll(MaterialCounter other) {
    for (TObjectIntIterator<Material> iter = other.counts.iterator(); iter.hasNext(); ) {
      iter.advance();
      counts.adjustOrPutValue(iter.key(), iter.value(), iter.value());
    }
  }

  public Iterable<Material> materials() {
    return new Iterable<Material>() {
      @Override
      public Iterator<Material> iterator() {
        return new Iterator<Material>() {
          final Iterator<Material> iterator = counts.keySet().iterator();

          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public Material next() {
            return iterator.next();
          }

          @Override
          public void remove() {
            iterator.remove();
          }
        };
      }
    };
  }
}
