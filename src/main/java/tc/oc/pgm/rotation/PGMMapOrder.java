package tc.oc.pgm.rotation;

import tc.oc.pgm.map.PGMMap;

/**
 * An order of {@link PGMMap}s, which can either be a {@link FixedPGMMapOrder} or a {@link
 * RandomPGMMapOrder}
 */
public interface PGMMapOrder {
  PGMMap popNextMap();

  PGMMap getNextMap();

  void setNextMap(PGMMap map);
}
