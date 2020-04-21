package tc.oc.pgm.util.material;

import static tc.oc.pgm.util.material.MaterialEncoder.decodeMaterial;
import static tc.oc.pgm.util.material.MaterialEncoder.encodeMaterial;

import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.Iterator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;

/** Efficiently counts distinct {@link MaterialData}s */
public class MaterialCounter {
  private final TIntIntMap counts;

  private MaterialCounter(TIntIntMap counts) {
    this.counts = counts;
  }

  public MaterialCounter() {
    this(new TIntIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, 0));
  }

  public MaterialCounter(MaterialCounter other) {
    this(new TIntIntHashMap(other.counts));
  }

  public boolean contains(int encodedMaterial) {
    return counts.containsKey(encodedMaterial);
  }

  public boolean contains(int typeId, byte metadata) {
    return contains(encodeMaterial(typeId, metadata));
  }

  public boolean contains(MaterialData material) {
    return contains(encodeMaterial(material));
  }

  public int get(int encodedMaterial) {
    return counts.get(encodedMaterial);
  }

  public int get(int typeId, byte metadata) {
    return get(encodeMaterial(typeId, metadata));
  }

  public int get(MaterialData material) {
    return get(encodeMaterial(material));
  }

  public int increment(int encodedMaterial, int count) {
    return counts.adjustOrPutValue(encodedMaterial, count, count);
  }

  public int increment(int typeId, byte metadata, int count) {
    return increment(encodeMaterial(typeId, metadata), count);
  }

  public int increment(MaterialData material, int count) {
    return increment(encodeMaterial(material), count);
  }

  public int increment(BlockState block, int count) {
    return increment(encodeMaterial(block), count);
  }

  public int analyze(World world, BlockVector pos) {
    return increment(encodeMaterial(world, pos), 1);
  }

  public int analyze(Location location) {
    return increment(encodeMaterial(location), 1);
  }

  public int analyze(Block block) {
    return increment(encodeMaterial(block), 1);
  }

  public int analyze(BlockState block) {
    return increment(encodeMaterial(block), 1);
  }

  public void analyze(World world, Iterator<BlockVector> blocks) {
    while (blocks.hasNext()) {
      analyze(world, blocks.next());
    }
  }

  public void clear() {
    counts.clear();
  }

  public void addAll(MaterialCounter other) {
    for (TIntIntIterator iter = other.counts.iterator(); iter.hasNext(); ) {
      iter.advance();
      counts.adjustOrPutValue(iter.key(), iter.value(), iter.value());
    }
  }

  public Iterable<MaterialData> materials() {
    return new Iterable<MaterialData>() {
      @Override
      public Iterator<MaterialData> iterator() {
        return new Iterator<MaterialData>() {
          final TIntIterator iter = counts.keySet().iterator();

          @Override
          public boolean hasNext() {
            return iter.hasNext();
          }

          @Override
          public MaterialData next() {
            return decodeMaterial(iter.next());
          }

          @Override
          public void remove() {
            iter.remove();
          }
        };
      }
    };
  }
}
