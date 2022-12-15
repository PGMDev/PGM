package tc.oc.pgm.rotation.pools;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.vote.MapPoll;
import tc.oc.pgm.rotation.vote.MapVotePicker;

public class VotingPool extends MapPool {

  // Arbitrary default of 1 in 5 players liking each map
  public static final double DEFAULT_SCORE = 0.2;
  // How much score to add/remove on a map every cycle
  public final double ADJUST_FACTOR;

  // The algorithm used to pick the maps for next vote.
  public final MapVotePicker mapPicker;

  // The current rating of maps. Eventually should be persisted elsewhere.
  private final Map<MapInfo, Double> mapScores = new HashMap<>();

  private MapPoll currentPoll;

  public VotingPool(
      MapPoolType type, String name, MapPoolManager manager, ConfigurationSection section) {
    super(type, name, manager, section);

    this.ADJUST_FACTOR = DEFAULT_SCORE / maps.size();

    this.mapPicker = MapVotePicker.of(manager, section);
    for (MapInfo map : maps) mapScores.put(map, DEFAULT_SCORE);
  }

  public VotingPool(
      MapPoolType type,
      MapPoolManager manager,
      String name,
      boolean enabled,
      int players,
      boolean dynamic,
      Duration cycleTime,
      List<MapInfo> maps) {
    super(type, name, manager, enabled, players, dynamic, cycleTime, maps);
    this.ADJUST_FACTOR = DEFAULT_SCORE / maps.size();
    this.mapPicker = MapVotePicker.of(manager, null);
    for (MapInfo map : maps) mapScores.put(map, DEFAULT_SCORE);
  }

  public MapPoll getCurrentPoll() {
    return currentPoll;
  }

  public double getMapScore(MapInfo map) {
    return mapScores.get(map);
  }

  /** Ticks scores for all maps, making them go slowly towards DEFAULT_WEIGHT. */
  private void tickScores(MapInfo currentMap) {
    // If the current map isn't from this pool, ignore ticking
    if (!mapScores.containsKey(currentMap)) return;
    mapScores.replaceAll(
        (mapScores, value) ->
            value > DEFAULT_SCORE
                ? Math.max(value - ADJUST_FACTOR, DEFAULT_SCORE)
                : Math.min(value + ADJUST_FACTOR, DEFAULT_SCORE));
    mapScores.put(currentMap, 0d);
  }

  private void updateScores(Map<MapInfo, Set<UUID>> votes) {
    double voters = votes.values().stream().flatMap(Collection::stream).distinct().count();
    if (voters == 0) return; // Literally no one voted
    votes.forEach((m, v) -> mapScores.put(m, Math.max(v.size() / voters, Double.MIN_VALUE)));
  }

  @Override
  public MapInfo popNextMap() {
    if (currentPoll == null) return getRandom();

    MapInfo map = currentPoll.finishVote();
    updateScores(currentPoll.getVotes());
    manager.getVoteOptions().clearMaps();
    currentPoll = null;
    return map != null ? map : getRandom();
  }

  @Override
  public MapInfo getNextMap() {
    return null;
  }

  @Override
  public void setNextMap(MapInfo map) {
    if (map != null && currentPoll != null) {
      currentPoll.cancel();
      currentPoll = null;
    }
  }

  @Override
  public void unloadPool(Match match) {
    tickScores(match.getMap());
  }

  @Override
  public void matchEnded(Match match) {
    tickScores(match.getMap());
    match
        .getExecutor(MatchScope.LOADED)
        .schedule(
            () -> {
              // Start poll here, to avoid starting it if you set next another map.
              if (manager.getOverriderMap() != null) return;
              // If there is a restart queued, don't start a vote
              if (RestartManager.isQueued()) return;

              currentPoll =
                  new MapPoll(match, mapPicker.getMaps(manager.getVoteOptions(), mapScores));
            },
            5,
            TimeUnit.SECONDS);
  }
}
