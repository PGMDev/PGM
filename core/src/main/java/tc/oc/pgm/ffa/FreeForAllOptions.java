package tc.oc.pgm.ffa;

import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.pgm.api.filter.Filter;

public record FreeForAllOptions(
    int minPlayers,
    int maxPlayers,
    int maxOverfill,
    NameTagVisibility nameTagVisibility,
    Filter nameTagVisibilityFilter,
    boolean colors) {}
