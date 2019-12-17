package tc.oc.pgm.rotation;

import java.util.*;
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
  }

  @Override
  public PGMMap popNextMap() {
    Iterator<Match> iterator = matchManager.getMatches().iterator();
    PGMMap current = iterator.hasNext() ? iterator.next().getMap() : null;

    List<PGMMap> maps = new ArrayList<>(PGM.get().getMapLibrary().getMaps());
    do {
      Collections.shuffle(maps);
      nextMap = maps.get(0);
    } while (maps.size() > 1 && Objects.equals(current, nextMap));

    return nextMap;
  }

  @Override
  public PGMMap getNextMap() {
    return nextMap;
  }

  @Override
  public void setNextMap(PGMMap map) {
    nextMap = map;
  }
}
