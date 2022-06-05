package tc.oc.pgm.rotation.vote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;

/**
 * Responsible for picking the set of maps that will be on the vote. It's able to apply any
 * arbitrary rule to how the maps are picked from the available ones.
 */
public class MapVotePicker {

  // Number of maps in the vote, unless not enough maps in pool
  public static final int MAX_VOTE_OPTIONS = 5;
  public static final int MIN_CUSTOM_VOTE_OPTIONS = 2;

  private final double gamemodeMultiplier;
  private final double weightPower;

  public MapVotePicker(ConfigurationSection config) {
    // Create dummy config to read defaults off of.
    if (config == null) config = new MemoryConfiguration();

    this.gamemodeMultiplier = config.getDouble("repeated-gamemode-multiplier", 1.0d);
    this.weightPower = config.getDouble("weight-power", 2.0d);
    // TODO: define format for online-playercount bias
  }

  /**
   * Get a list of maps to vote on, given voting options and map of scores
   *
   * @param options custom voting options currently available
   * @param scores maps and their respective scores
   * @return list of maps to include in the vote
   */
  public List<MapInfo> getMaps(VotePoolOptions options, Map<MapInfo, Double> scores) {
    if (options.shouldOverride())
      return getMaps(new ArrayList<>(), options.getCustomVoteMapWeighted());

    List<MapInfo> maps = new ArrayList<>(options.getCustomVoteMaps());
    return getMaps(maps, scores);
  }

  protected List<MapInfo> getMaps(@Nullable List<MapInfo> selected, Map<MapInfo, Double> scores) {
    if (selected == null) selected = new ArrayList<>();

    List<MapInfo> unmodifiable = Collections.unmodifiableList(selected);
    while (selected.size() < MAX_VOTE_OPTIONS) {
      MapInfo map = getMap(unmodifiable, scores);

      if (map == null) break; // Ran out of maps!
      selected.add(map);
    }

    return selected;
  }

  protected MapInfo getMap(List<MapInfo> selected, Map<MapInfo, Double> mapScores) {
    NavigableMap<Double, MapInfo> cumulativeScores = new TreeMap<>();
    double maxWeight = 0;
    for (Map.Entry<MapInfo, Double> map : mapScores.entrySet()) {
      double weight = getWeight(selected, map.getKey(), map.getValue());
      if (weight > 0) cumulativeScores.put(maxWeight += weight, map.getKey());
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
   * @param score The score of the map, from player votes
   * @return random weight for the map
   */
  public double getWeight(@Nullable List<MapInfo> selected, @Nullable MapInfo map, double score) {
    if (selected == null || map == null || selected.contains(map) || score <= 0) return 0;

    double weight = score;

    // Remove score if same gamemode is already in the vote
    if (gamemodeMultiplier != 1.0 && !selected.isEmpty()) {
      List<MapTag> gamemodes =
          map.getTags().stream().filter(MapTag::isGamemode).collect(Collectors.toList());

      for (MapInfo otherMap : selected) {
        if (!Collections.disjoint(gamemodes, otherMap.getTags())) weight *= gamemodeMultiplier;
      }
    }

    // TODO: apply weight based on playercount

    // Apply curve to bump up high weights and kill lower weights
    weight = Math.pow(weight, weightPower);

    // Use MIN_VALUE so that weight isn't exactly 0.
    // That allows for the map to be used if nothing else exists.
    return Math.max(weight, Double.MIN_VALUE);
  }
}
