package tc.oc.pgm.util;

import javax.annotation.Nullable;
import tc.oc.pgm.match.MatchPlayer;

public class MatchPlayers {
  public static boolean canInteract(@Nullable MatchPlayer player) {
    return player != null && player.canInteract();
  }

  public static boolean cannotInteract(@Nullable MatchPlayer player) {
    return player != null && !player.canInteract();
  }
}
