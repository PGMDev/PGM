package tc.oc.pgm.rotation;

import com.google.common.collect.Lists;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.exception.MapMissingException;

/**
 * A type of {@link MapOrder} which orders maps randomly, setting aleatory maps for pgm to be able
 * to continue cycling
 */
public class RandomMapOrder implements MapOrder {

  private final Random random;
  private final Deque<WeakReference<MapInfo>> deque;

  public RandomMapOrder() {
    this.random = new Random();
    this.deque = new ArrayDeque<>();
  }

  private void refresh() {
    if (!deque.isEmpty()) return; // Only re-populate when deque is empty

    final List<MapInfo> maps = Lists.newArrayList(PGM.get().getMapLibrary().getMaps());
    if (maps.isEmpty())
      throw new RuntimeException(
          new MapMissingException("map library", "No maps found to set next"));

    Collections.shuffle(maps, random);
    maps.stream().map(WeakReference::new).forEachOrdered(deque::addLast);
  }

  @Override
  public MapInfo popNextMap() {
    final WeakReference<MapInfo> ref = deque.pollFirst();
    if (ref != null) {
      final MapInfo info = ref.get();
      if (info != null) {
        return info;
      }
    }

    refresh();
    return popNextMap();
  }

  @Override
  public MapInfo getNextMap() {
    final WeakReference<MapInfo> ref = deque.peekFirst();
    if (ref != null) {
      final MapInfo info = ref.get();
      if (info != null) {
        return info;
      }
    }

    refresh();
    return getNextMap();
  }

  @Override
  public void setNextMap(MapInfo map) {
    // Set next maps are sent to the front of the deque
    deque.addFirst(new WeakReference<>(map));
  }
}
