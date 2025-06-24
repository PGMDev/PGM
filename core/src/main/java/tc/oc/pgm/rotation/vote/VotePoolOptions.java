package tc.oc.pgm.rotation.vote;

import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.rotation.pools.VotingPool;

public class VotePoolOptions {

  // Added maps w/ nullable playerId to be used in custom vote selection
  private final Map<MapInfo, UUID> customVoteMaps;
  // Whether custom map selection should replace existing entries
  private boolean replace;

  public VotePoolOptions() {
    this.customVoteMaps = Maps.newHashMap();
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

  public boolean canAddMap() {
    return customVoteMaps.size() < MapVotePicker.MAX_VOTE_OPTIONS;
  }

  public boolean isMapAdded(MapInfo info) {
    return customVoteMaps.containsKey(info);
  }

  public boolean addMap(MapInfo map) {
    return addMap(map, null);
  }

  public boolean addMap(MapInfo map, @Nullable UUID playerId) {
    if (canAddMap()) {
      this.customVoteMaps.put(map, null);
      return true;
    }
    return false;
  }

  public boolean removeMap(MapInfo map) {
    boolean present = this.customVoteMaps.containsKey(map);
    this.customVoteMaps.remove(map);
    return present;
  }

  public void clearMaps() {
    customVoteMaps.clear();
  }

  public Set<MapInfo> getCustomVoteMaps() {
    return Collections.unmodifiableSet(customVoteMaps.keySet());
  }

  public Map<MapInfo, VoteData> getCustomVoteMapsWeighted() {
    return customVoteMaps.keySet().stream()
        .collect(Collectors.toMap(
            map -> map, map -> VoteData.of(1, VotingPool.DEFAULT_SCORE, map, false)));
  }
}
