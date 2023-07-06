package tc.oc.pgm.util.material;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.block.BlockState;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.MaterialDataProvider;

/** Efficiently counts distinct {@link MaterialData}s */
public class MaterialCounter {
  private static int ENCODED_NULL_MATERIAL = -1;
  private final Map<Integer, Integer> counts;

  public MaterialCounter() {
    this.counts = new HashMap<>();
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
    Integer oldCount = counts.get(encodedMaterial);
    if (oldCount == null) oldCount = 0;
    int newCount = oldCount + count;
    counts.put(encodedMaterial, newCount);

    return newCount;
  }

  public int increment(BlockState block, int count) {
    MaterialData material = MaterialDataProvider.from(block);
    return increment(material == null ? ENCODED_NULL_MATERIAL : material.encode(), count);
  }

  public Iterable<MaterialData> materials() {
    return () ->
        new Iterator<MaterialData>() {
          final Iterator<Integer> iter = counts.keySet().iterator();

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
