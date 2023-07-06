package tc.oc.pgm.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.entity.Player;

public class PGMFastBoard extends FastBoard {
  public PGMFastBoard(Player player) {
    super(player);
  }

  @Override
  public boolean hasLinesMaxLength() {
    return true;
  }
}
