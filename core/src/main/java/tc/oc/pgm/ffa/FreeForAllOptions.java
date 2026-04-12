package tc.oc.pgm.ffa;

import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.pgm.api.filter.Filter;

public class FreeForAllOptions {
  public final int minPlayers;
  public final int maxPlayers;
  public final int maxOverfill;
  public final NameTagVisibility nameTagVisibility;
  public final Filter nameTagVisibilityFilter;
  public final boolean colors;

  public FreeForAllOptions(
      int minPlayers,
      int maxPlayers,
      int maxOverfill,
      NameTagVisibility nameTagVisibility,
      Filter nameTagVisibilityFilter,
      boolean colors) {
    this.minPlayers = minPlayers;
    this.maxPlayers = maxPlayers;
    this.maxOverfill = maxOverfill;
    this.nameTagVisibility = nameTagVisibility;
    this.nameTagVisibilityFilter = nameTagVisibilityFilter;
    this.colors = colors;
  }
}
