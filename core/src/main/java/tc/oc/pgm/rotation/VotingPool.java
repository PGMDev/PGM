package tc.oc.pgm.rotation;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.restart.RestartManager;

public class VotingPool extends MapPool {

  // Number of maps in the vote, unless not enough maps in pool
  public static final int MAX_VOTE_OPTIONS = 5;
  // Number of maps required for a custom vote (/vote)
  public static final int MIN_CUSTOM_VOTE_OPTIONS = 2;
  // If maps were single voted, it would avg to this default
  private static final double DEFAULT_WEIGHT = 1d / MAX_VOTE_OPTIONS;

  // Amount of maps to display on vote
  private final int VOTE_SIZE;
  private final double ADJUST_FACTOR;
  private final Map<MapInfo, Double> mapScores = new HashMap<>();

  private MapPoll currentPoll;

  /** Override pool is used when custom maps are defined, does not count for regular vote score * */
  private Set<MapInfo> customVoteMaps = Sets.newHashSet();

  /** Whether custom vote should replace existing maps or override the poll * */
  private boolean replaceMaps = true;

  public VotingPool(MapPoolManager manager, ConfigurationSection section, String name) {
    super(manager, section, name);
    VOTE_SIZE = Math.min(MAX_VOTE_OPTIONS, maps.size() - 1);
    ADJUST_FACTOR = 1d / (maps.size() * MAX_VOTE_OPTIONS);

    for (MapInfo map : maps) {
      mapScores.put(map, DEFAULT_WEIGHT);
    }
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
            value > DEFAULT_WEIGHT
                ? Math.max(value - ADJUST_FACTOR, DEFAULT_WEIGHT)
                : Math.min(value + ADJUST_FACTOR, DEFAULT_WEIGHT));
    mapScores.put(currentMap, 0d);
  }

  @Override
  public MapInfo popNextMap() {
    if (currentPoll == null) return getRandom();

    MapInfo map = currentPoll.finishVote();
    customVoteMaps.clear();
    currentPoll = null;
    return map != null ? map : getRandom();
  }

  @Override
  public MapInfo getNextMap() {
    return null;
  }

  @Override
  public void setNextMap(MapInfo map) {
    currentPoll = null;
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
                  shouldOverride()
                      ? new MapPoll(
                          match,
                          getCustomVoteMapWeighted(),
                          Collections.emptySet(),
                          Math.min(MAX_VOTE_OPTIONS, customVoteMaps.size()))
                      : new MapPoll(match, mapScores, customVoteMaps, VOTE_SIZE);

              match.getPlayers().forEach(viewer -> currentPoll.sendBook(viewer, false));
            },
            5,
            TimeUnit.SECONDS);
  }

  public boolean addCustomVoteMap(MapInfo info) {
    if (customVoteMaps.size() < MAX_VOTE_OPTIONS) {
      this.customVoteMaps.add(info);
      return true;
    }
    return false;
  }

  public boolean removeCustomVote(MapInfo map) {
    return this.customVoteMaps.remove(map);
  }

  public Set<MapInfo> getCustomVoteMaps() {
    return customVoteMaps;
  }

  public boolean isCustomMapSelected(MapInfo info) {
    return customVoteMaps.stream().anyMatch(s -> s.getName().equalsIgnoreCase(info.getName()));
  }

  private boolean shouldOverride() {
    return customVoteMaps.size() >= MIN_CUSTOM_VOTE_OPTIONS && !getVoteMode();
  }

  public boolean toggleVoteMode() {
    this.replaceMaps = !replaceMaps;
    return replaceMaps;
  }

  /** @return true if replace, false if override */
  public boolean getVoteMode() {
    return replaceMaps;
  }

  private Map<MapInfo, Double> getCustomVoteMapWeighted() {
    Map<MapInfo, Double> maps = Maps.newHashMap();
    customVoteMaps.forEach(map -> maps.put(map, DEFAULT_WEIGHT));
    return maps;
  }
}
