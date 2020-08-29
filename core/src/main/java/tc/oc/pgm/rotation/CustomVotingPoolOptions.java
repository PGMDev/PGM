package tc.oc.pgm.rotation;

import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import tc.oc.pgm.api.map.MapInfo;

public class CustomVotingPoolOptions {

  // Set of maps to be used in custom vote selection
  private Set<MapInfo> customVoteMaps;

  // Whether custom map selection should replace existing entries
  private boolean replace;

  public CustomVotingPoolOptions() {
    this.customVoteMaps = Sets.newHashSet();
    this.replace = true;
  }

  public boolean shouldOverride() {
    return customVoteMaps.size() >= VotingPool.MIN_CUSTOM_VOTE_OPTIONS && !replace;
  }

  public boolean isReplace() {
    return replace;
  }

  public boolean toggleMode() {
    this.replace = !replace;
    return replace;
  }

  public boolean addVote(MapInfo map) {
    if (customVoteMaps.size() < VotingPool.MAX_VOTE_OPTIONS) {
      this.customVoteMaps.add(map);
      return true;
    }
    return false;
  }

  public boolean removeMap(MapInfo map) {
    return this.customVoteMaps.remove(map);
  }

  public Set<MapInfo> getCustomVoteMaps() {
    return customVoteMaps;
  }

  public boolean isAdded(MapInfo info) {
    return customVoteMaps.stream().anyMatch(s -> s.getName().equalsIgnoreCase(info.getName()));
  }

  public void clear() {
    customVoteMaps.clear();
  }

  public Map<MapInfo, Double> getCustomVoteMapWeighted() {
    return customVoteMaps.stream()
        .collect(Collectors.toMap(map -> map, x -> VotingPool.DEFAULT_WEIGHT));
  }
}
