package tc.oc.pgm.rotation.vote;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.rotation.CustomVoteEntry;
import tc.oc.pgm.rotation.pools.VotingPool;

public class VotePoolOptions {

  private Set<CustomVoteEntry> customVoteMaps;

  // Whether custom map selection should replace existing entries
  private boolean replace;

  public VotePoolOptions() {
    this.customVoteMaps = Sets.newHashSet();
    this.replace = true;
  }

  public boolean shouldOverride() {
    return customVoteMaps.size() >= MapVotePicker.MIN_CUSTOM_VOTE_OPTIONS && !replace;
  }

  public boolean isReplace() {
    return replace;
  }

  public boolean toggleMode() {
    this.replace = !replace;
    return replace;
  }

  public boolean canAddVote() {
    return customVoteMaps.size() < MapVotePicker.MAX_VOTE_OPTIONS;
  }

  public boolean addVote(MapInfo map, UUID playerId, boolean identify) {
    if (customVoteMaps.size() < MapVotePicker.MAX_VOTE_OPTIONS) {
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
    return customVoteMaps.stream()
        .collect(Collectors.toMap(map -> map.getMap(), x -> VotingPool.DEFAULT_SCORE));
  }
}
