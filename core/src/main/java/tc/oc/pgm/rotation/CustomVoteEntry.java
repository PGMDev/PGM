package tc.oc.pgm.rotation;

import java.util.UUID;
import tc.oc.pgm.api.map.MapInfo;

public class CustomVoteEntry {

  private final MapInfo map;
  private final boolean identify;
  private final UUID playerId;

  public CustomVoteEntry(MapInfo map, boolean identify, UUID playerId) {
    this.map = map;
    this.identify = identify;
    this.playerId = playerId;
  }

  public MapInfo getMap() {
    return map;
  }

  public boolean isIdentified() {
    return identify;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof CustomVoteEntry) {
      return ((CustomVoteEntry) other).getMap().equals(getMap());
    }
    return false;
  }
}
