package tc.oc.pgm.rotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;

/**
 * A type of {@link MapOrder} which orders maps randomly, setting aleatory maps for pgm to be able
 * to continue cycling
 */
public class RandomMapOrder implements MapOrder {

  private MatchManager matchManager;
  private MapInfo nextMap;

  public RandomMapOrder(MatchManager matchManager) {
    this.matchManager = matchManager;
    this.nextMap = getRandomMap();
  }

  private MapInfo getRandomMap() {
    Iterator<Match> iterator = matchManager.getMatches().iterator();
    MapInfo current = iterator.hasNext() ? iterator.next().getMap().getInfo() : null;
    List<MapContext> maps = new ArrayList<>(PGM.get().getMapLibrary().getMaps());
    Collections.shuffle(maps);

    for (MapContext map : maps) {
      if (map.getInfo() != current) return map.getInfo();
    }
    return maps.get(0).getInfo();
  }

  @Override
  public MapInfo popNextMap() {
    MapInfo map = nextMap;
    this.nextMap = getRandomMap();
    return map;
  }

  @Override
  public MapInfo getNextMap() {
    return nextMap;
  }

  @Override
  public void setNextMap(MapInfo map) {
    this.nextMap = map;
  }
}
