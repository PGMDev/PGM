package tc.oc.pgm.util.material;

import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.Iterator;
import org.bukkit.block.BlockState;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.MaterialDataProvider;
import tc.oc.pgm.util.nms.material.legacy.MaterialDataLegacy;

/** Efficiently counts distinct {@link MaterialDataLegacy}s */
public class MaterialCounter {
  private static int ENCODED_NULL_MATERIAL = -1;
  private final TIntIntMap counts;

  private MaterialCounter(TIntIntMap counts) {
    this.counts = counts;
  }

  public MaterialCounter() {
    this(new TIntIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, 0));
  }

  public boolean contains(int encodedMaterial) {
    return counts.containsKey(encodedMaterial);
  }

  public boolean contains(MaterialData material) {
    return contains(material == null ? ENCODED_NULL_MATERIAL : material.encode());
  }

  public int get(int encodedMaterial) {
    return counts.get(encodedMaterial);
  }

  public int get(MaterialData material) {
    return get(material == null ? ENCODED_NULL_MATERIAL : material.encode());
  }

  public int increment(int encodedMaterial, int count) {
    return counts.adjustOrPutValue(encodedMaterial, count, count);
  }

  public int increment(BlockState block, int count) {
    MaterialData material = MaterialDataProvider.from(block);
    return increment(material == null ? ENCODED_NULL_MATERIAL : material.encode(), count);
  }

  public Iterable<MaterialData> materials() {
    return () ->
        new Iterator<MaterialData>() {
          final TIntIterator iter = counts.keySet().iterator();

          @Override
          public boolean hasNext() {
            return iter.hasNext();
          }

          @Override
          public MaterialData next() {
            int encoded = iter.next();
            if (encoded == ENCODED_NULL_MATERIAL) return null;
            return MaterialDataProvider.from(encoded);
          }

          @Override
          public void remove() {
            iter.remove();
          }
        };
  }
}
