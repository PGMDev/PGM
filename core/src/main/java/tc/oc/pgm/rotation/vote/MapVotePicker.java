package tc.oc.pgm.rotation.vote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import net.objecthunter.exp4j.ExpressionContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.VotingPool;
import tc.oc.pgm.util.math.Formula;

/**
 * Responsible for picking the set of maps that will be on the vote. It's able to apply any
 * arbitrary rule to how the maps are picked from the available ones.
 */
public class MapVotePicker {

  // Number of maps in the vote, unless not enough maps in pool
  public static final int MAX_VOTE_OPTIONS = 5;
  public static final int MIN_CUSTOM_VOTE_OPTIONS = 2;

  // A 0 that prevents arbitrarily low values with tons of precision, which cause issues when mixed
  // with larger numbers.
  private static final double MINIMUM_WEIGHT = 0.000001;

  private static final Formula<MapVoteContext> DEFAULT_MODIFIER =
      c -> Math.pow(c.getVariable("score"), 2);

  private final MapPoolManager manager;
  private final VotingPool.VoteConstants constants;
  private final Formula<MapVoteContext> modifier;

  public static MapVotePicker of(
      MapPoolManager manager, VotingPool.VoteConstants constants, ConfigurationSection config) {
    // Create dummy config to read defaults off of.
    if (config == null) config = new MemoryConfiguration();

    Formula<MapVoteContext> formula = DEFAULT_MODIFIER;
    try {
      formula =
          Formula.of(config.getString("modifier"), MapVoteContext.variables(), DEFAULT_MODIFIER);
    } catch (IllegalArgumentException e) {
      PGM.get()
          .getLogger()
          .log(Level.SEVERE, "Failed to load vote picker modifier formula, using fallback", e);
    }

    return new MapVotePicker(manager, constants, formula);
  }

  private MapVotePicker(
      MapPoolManager manager,
      VotingPool.VoteConstants constants,
      Formula<MapVoteContext> modifier) {
    this.manager = manager;
    this.constants = constants;
    this.modifier = modifier;
  }

  /**
   * Get a list of maps to vote on, given voting options and map of scores
   *
   * @param options custom voting options currently available
   * @param scores maps and their respective scores
   * @return list of maps to include in the vote
   */
  public List<MapInfo> getMaps(VotePoolOptions options, Map<MapInfo, VoteData> scores) {
    if (options.shouldOverride())
      return getMaps(new ArrayList<>(), options.getCustomVoteMapsWeighted());

    List<MapInfo> maps = new ArrayList<>(options.getCustomVoteMaps());
    return getMaps(maps, scores);
  }

  protected List<MapInfo> getMaps(@Nullable List<MapInfo> selected, Map<MapInfo, VoteData> scores) {
    if (selected == null) selected = new ArrayList<>();

    List<MapInfo> unmodifiable = Collections.unmodifiableList(selected);
    while (selected.size() < constants.voteOptions()) {
      MapInfo map = getMap(unmodifiable, scores);

      if (map == null) break; // Ran out of maps!
      selected.add(map);
    }

    return selected;
  }

  public MapInfo getMap(List<MapInfo> selected, Map<MapInfo, VoteData> mapScores) {
    NavigableMap<Double, MapInfo> cumulativeScores = new TreeMap<>();
    double maxWeight = 0;
    for (Map.Entry<MapInfo, VoteData> map : mapScores.entrySet()) {
      double weight = getWeight(selected, map.getKey(), map.getValue());
      if (weight > MINIMUM_WEIGHT) cumulativeScores.put(maxWeight += weight, map.getKey());
    }
    Map.Entry<Double, MapInfo> selectedMap =
        cumulativeScores.higherEntry(Math.random() * maxWeight);
    return selectedMap == null ? null : selectedMap.getValue();
  }

  /**
   * Get the weight for a specific map, given it's score
   *
   * @param selected The list of selected maps so far
   * @param map The map being considered
   * @param data The vote data of the map, from player votes and config
   * @return random weight for the map
   */
  public double getWeight(@Nullable List<MapInfo> selected, @NotNull MapInfo map, VoteData data) {
    if ((selected != null && selected.contains(map))
        || data.getScore() <= constants.scoreMinToVote()
        || data.isOnCooldown(constants)) return 0;

    var context = new MapVoteContext(
        data.getScore(),
        getRepeatedGamemodes(selected, map),
        map.getMaxPlayers().stream().mapToInt(i -> i).sum(),
        manager.getActivePlayers(null));

    return Math.max(modifier.applyAsDouble(context) * data.getWeight(), 0);
  }

  private double getRepeatedGamemodes(List<MapInfo> selected, MapInfo map) {
    if (selected == null || selected.isEmpty()) return 0;
    Collection<Gamemode> gamemodes = map.getGamemodes();

    return selected.stream()
        .filter(s -> !Collections.disjoint(gamemodes, s.getGamemodes()))
        .count();
  }

  private static final class MapVoteContext extends ExpressionContext.Impl {
    public MapVoteContext(double score, double sameGamemode, double mapsize, double players) {
      super(
          Map.of(
              "score", score,
              "same_gamemode", sameGamemode,
              "mapsize", mapsize,
              "players", players),
          null);
    }

    static Set<String> variables() {
      return new MapVoteContext(0, 0, 0, 0).getVariables();
    }
  }
}
