package tc.oc.pgm.rotation;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import tc.oc.pgm.api.map.MapInfo;

public class CustomVotingPoolOptions {

  // Set of maps to be used in custom vote selection
  private Set<CustomVoteEntry> customVoteMaps;

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

  public boolean addVote(MapInfo map, UUID playerId, boolean identify) {
    if (customVoteMaps.size() < VotingPool.MAX_VOTE_OPTIONS) {
      this.customVoteMaps.add(new CustomVoteEntry(map, identify, playerId));
      return true;
    }
    return false;
  }

  public boolean removeMap(MapInfo map) {
    Optional<CustomVoteEntry> entry =
        getCustomVotes().stream().filter(s -> s.getMap().equals(map)).findFirst();
    if (entry.isPresent()) {
      return customVoteMaps.remove(entry.get());
    }
    return false;
  }

  public Set<CustomVoteEntry> getCustomVotes() {
    return customVoteMaps;
  }

  public Map<MapInfo, UUID> getOverrideMaps() {
    Map<MapInfo, UUID> overrides = Maps.newHashMap();
    customVoteMaps.stream()
        .forEach(
            entry -> {
              overrides.put(entry.getMap(), entry.isIdentified() ? entry.getPlayerId() : null);
            });
    return overrides;
  }

  public Set<MapInfo> getCustomVoteMaps() {
    return customVoteMaps.stream().map(CustomVoteEntry::getMap).collect(Collectors.toSet());
  }

  public boolean isAdded(MapInfo info) {
    return getCustomVoteMaps().stream().anyMatch(s -> s.getName().equalsIgnoreCase(info.getName()));
  }

  public void clear() {
    customVoteMaps.clear();
  }

  public Map<MapInfo, Double> getCustomVoteMapWeighted() {
    return getCustomVoteMaps().stream()
        .collect(Collectors.toMap(map -> map, x -> VotingPool.DEFAULT_WEIGHT));
  }
}
