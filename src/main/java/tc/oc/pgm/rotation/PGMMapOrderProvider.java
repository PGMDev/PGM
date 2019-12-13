package tc.oc.pgm.rotation;

import tc.oc.pgm.map.PGMMap;

public interface PGMMapOrderProvider {
  PGMMap popNextMap();

  PGMMap getNextMap();

  PGMMap popFallbackMap();

  void setNextMap(PGMMap map);
}
