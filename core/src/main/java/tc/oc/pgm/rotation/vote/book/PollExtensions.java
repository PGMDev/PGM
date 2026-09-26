package tc.oc.pgm.rotation.vote.book;

import java.util.UUID;
import tc.oc.pgm.api.map.MapInfo;

public interface PollExtensions {

  int getVotingPower(UUID player);

  boolean isSpecialMap(MapInfo map);
}
