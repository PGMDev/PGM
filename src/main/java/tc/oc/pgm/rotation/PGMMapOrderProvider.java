package tc.oc.pgm.rotation;

import tc.oc.pgm.map.PGMMap;

public interface PGMMapOrderProvider {
  PGMMap getNextMapByRotation();

  PGMMap getNextMapRandomly();
}
