package tc.oc.pgm.rotation.pools;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.objecthunter.exp4j.ExpressionContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.vote.MapPoll;
import tc.oc.pgm.rotation.vote.MapVotePicker;
import tc.oc.pgm.util.math.Formula;

public class VotingPool extends MapPool {

  // Arbitrary default of 1 in 5 players liking each map
  public static final double DEFAULT_SCORE = 0.2;

  public final VoteConstants constants;
  // The algorithm used to pick the maps for next vote.
  public final MapVotePicker mapPicker;

  // The current rating of maps. Eventually should be persisted elsewhere.
  private final Map<MapInfo, Double> mapScores = new HashMap<>();

  private MapPoll currentPoll;

  public VotingPool(
      MapPoolType type, String name, MapPoolManager manager, ConfigurationSection section) {
    super(type, name, manager, section);
    this.constants = new VoteConstants(section, maps.size());

    this.mapPicker = MapVotePicker.of(manager, constants, section);
    for (MapInfo map : maps) mapScores.put(map, constants.defaultScore());
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
    this.constants = new VoteConstants(new MemoryConfiguration(), maps.size());
    this.mapPicker = MapVotePicker.of(manager, constants, null);
    for (MapInfo map : maps) mapScores.put(map, constants.defaultScore());
  }

  public MapPoll getCurrentPoll() {
    return currentPoll;
  }

  public double getMapScore(MapInfo map) {
    return mapScores.get(map);
  }

  /** Ticks scores for all maps, making them go slowly towards DEFAULT_WEIGHT. */
  private void tickScores(Match match) {
    // If the current map isn't from this pool, ignore ticking
    if (!mapScores.containsKey(match.getMap())) return;
    mapScores.replaceAll((mapScores, value) -> value > constants.defaultScore()
        ? Math.max(value - constants.scoreDecay(), constants.defaultScore())
        : Math.min(value + constants.scoreRise(), constants.defaultScore()));
    mapScores.put(
        match.getMap(), constants.scoreAfterPlay().applyAsDouble(new Context(match.getDuration())));
  }

  private void updateScores(Map<MapInfo, Set<UUID>> votes) {
    double voters =
        votes.values().stream().flatMap(Collection::stream).distinct().count();
    if (voters == 0) return; // Literally no one voted
    votes.forEach((m, v) ->
        mapScores.computeIfPresent(m, (a, b) -> constants.afterVoteScore(v.size() / voters)));
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
    tickScores(match);
  }

  @Override
  public void matchEnded(Match match) {
    tickScores(match);
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

  public record VoteConstants(
      int voteOptions,
      double defaultScore,
      double scoreDecay,
      double scoreRise,
      double scoreAfterVoteMin,
      double scoreAfterVoteMax,
      double scoreMinToVote,
      Formula<Context> scoreAfterPlay) {
    private VoteConstants(ConfigurationSection section, int mapAmount) {
      this(
          section.getInt("vote-options", MapVotePicker.MAX_VOTE_OPTIONS), // Show 5 maps
          section.getDouble("score.default", DEFAULT_SCORE), // Start at 20% each
          section.getDouble("score.decay", DEFAULT_SCORE / mapAmount), // Proportional to # of maps
          section.getDouble("score.rise", DEFAULT_SCORE / mapAmount), // Proportional to # of maps
          section.getDouble("score.min-after-vote", 0.01), // min = 1%, never fully discard the map
          section.getDouble("score.max-after-vote", 1), // max = 100%
          section.getDouble("score.min-for-vote", 0.01), // To even be voted, need at least 1%
          Formula.of(section.getString("score.after-playing"), Context.variables(), c -> 0));
    }

    public double afterVoteScore(double score) {
      return Math.max(Math.min(score, scoreAfterVoteMax), scoreAfterVoteMin);
    }
  }

  private static final class Context extends ExpressionContext.Impl {
    public Context(Duration length) {
      super(Map.of("play_minutes", length.toMillis() / 60_000d), null);
    }

    static Set<String> variables() {
      return new Context(Duration.ZERO).getVariables();
    }
  }
}
