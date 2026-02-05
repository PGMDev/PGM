package tc.oc.pgm.ffa;

import org.bukkit.scoreboard.NameTagVisibility;

public record FreeForAllOptions(
    int minPlayers,
    int maxPlayers,
    int maxOverfill,
    NameTagVisibility nameTagVisibility,
    boolean colors) {}
