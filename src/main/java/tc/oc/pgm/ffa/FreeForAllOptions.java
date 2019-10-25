package tc.oc.pgm.ffa;

import org.bukkit.scoreboard.NameTagVisibility;

public class FreeForAllOptions {
  public final int minPlayers;
  public final int maxPlayers;
  public final int maxOverfill;
  public final NameTagVisibility nameTagVisibility;

  public FreeForAllOptions(
      int minPlayers, int maxPlayers, int maxOverfill, NameTagVisibility nameTagVisibility) {
    this.minPlayers = minPlayers;
    this.maxPlayers = maxPlayers;
    this.maxOverfill = maxOverfill;
    this.nameTagVisibility = nameTagVisibility;
  }
}
