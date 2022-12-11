package tc.oc.pgm.rotation.vote;

import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.rotation.pools.VotingPool;

public class VotePoolOptions {

  // Set of maps w/ related data to be used in custom vote selection
  private final Set<CustomMapEntry> customVoteMaps;
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

  public boolean addVote(MapInfo map) {
    return addVote(map, null);
  }

  public boolean addVote(MapInfo map, @Nullable UUID playerId) {
    if (canAddVote()) {
      this.customVoteMaps.add(new CustomMapEntry(map, playerId));
      return true;
    }
    return false;
  }

  public boolean removeMap(MapInfo map) {
    return this.customVoteMaps.removeIf(e -> e.getMap().equals(map));
  }

  public Set<MapInfo> getCustomVoteMaps() {
    return customVoteMaps.stream().map(CustomMapEntry::getMap).collect(Collectors.toSet());
  }

  public boolean isAdded(MapInfo info) {
    return customVoteMaps.stream()
        .anyMatch(s -> s.getMap().getName().equalsIgnoreCase(info.getName()));
  }

  public void clear() {
    customVoteMaps.clear();
  }

  public Map<MapInfo, Double> getCustomVoteMapWeighted() {
    return customVoteMaps.stream()
        .collect(Collectors.toMap(map -> map.getMap(), x -> VotingPool.DEFAULT_SCORE));
  }

  public static class CustomMapEntry {
    private final MapInfo map;
    private final @Nullable UUID playerId;

    public CustomMapEntry(MapInfo map, @Nullable UUID playerId) {
      this.map = map;
      this.playerId = playerId;
    }

    public MapInfo getMap() {
      return map;
    }

    public boolean isIdentified() {
      return playerId != null;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof CustomMapEntry) {
        return ((CustomMapEntry) other).getMap().equals(getMap());
      }
      return false;
    }
  }
}
