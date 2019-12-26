package tc.oc.pgm.rotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.map.PGMMap;

/**
 * A type of {@link PGMMapOrder} which orders maps randomly, setting aleatory maps for pgm to be
 * able to continue cycling
 */
public class RandomPGMMapOrder implements PGMMapOrder {

  private MatchManager matchManager;
  private PGMMap nextMap;

  public RandomPGMMapOrder(MatchManager matchManager) {
    this.matchManager = matchManager;
    this.nextMap = getRandomMap();
  }

  private PGMMap getRandomMap() {
    Iterator<Match> iterator = matchManager.getMatches().iterator();
    PGMMap current = iterator.hasNext() ? iterator.next().getMap() : null;
    List<PGMMap> maps = new ArrayList<>(PGM.get().getMapLibrary().getMaps());
    Collections.shuffle(maps);

    for (PGMMap map : maps) {
      if (map != current) return map;
    }
    return maps.get(0);
  }

  @Override
  public PGMMap popNextMap() {
    PGMMap map = nextMap;
    this.nextMap = getRandomMap();
    return map;
  }

  @Override
  public PGMMap getNextMap() {
    return nextMap;
  }

  @Override
  public void setNextMap(PGMMap map) {
    this.nextMap = map;
  }
}
