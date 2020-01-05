package tc.oc.pgm.rotation;

import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.map.PGMMap;

import java.util.HashMap;
import java.util.Map;

public class VotingPool extends MapPool {

  // Number of maps in the vote, unless not enough maps in pool
  private static final int MAX_VOTE_OPTIONS = 5;
  // If maps were single voted, it would avg to this default
  private static final double DEFAULT_WEIGHT = 1d / MAX_VOTE_OPTIONS;

  // Amount of maps to display on vote
  private final int VOTE_SIZE;
  private final double ADJUST_FACTOR;
  private final Map<PGMMap, Double> mapScores = new HashMap<>();

  private MapPoll currentPoll;

  public VotingPool(MapPoolManager manager, ConfigurationSection section, String name) {
    super(manager, section, name);
    VOTE_SIZE = Math.min(MAX_VOTE_OPTIONS, maps.size() - 1);
    ADJUST_FACTOR = 1d / maps.size();

    for (PGMMap map : maps) {
      mapScores.put(map, DEFAULT_WEIGHT);
    }
  }

  public MapPoll getCurrentPoll() {
    return currentPoll;
  }

  public double getMapScore(PGMMap map) {
    return mapScores.get(map);
  }

  /** Ticks scores for all maps, making them go slowly towards DEFAULT_WEIGHT. */
  private void tickScores() {
    mapScores.replaceAll(
        (mapScores, value) ->
            value > DEFAULT_WEIGHT
                ? Math.max(value - ADJUST_FACTOR, DEFAULT_WEIGHT)
                : Math.min(value + ADJUST_FACTOR, DEFAULT_WEIGHT));
  }

  @Override
  public PGMMap popNextMap() {
    if (currentPoll == null) return getRandom();

    tickScores();
    PGMMap map = currentPoll.finishVote();
    currentPoll = null;
    return map != null ? map : getRandom();
  }

  @Override
  public PGMMap getNextMap() {
    return null;
  }

  @Override
  public void setNextMap(PGMMap map) {
    currentPoll = null;
  }

  @Override
  public void matchEnded(Match match) {
    mapScores.put(match.getMap(), 0d); // Ensure same map isn't in vote
    if (manager.getOverriderMap() != null) return;
    currentPoll = new MapPoll(match, mapScores, VOTE_SIZE);
    match
        .getScheduler(MatchScope.LOADED)
        .runTaskLater(20 * 5, () -> match.getPlayers().forEach(currentPoll::sendBook));
  }

}
