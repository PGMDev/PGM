package tc.oc.pgm.spawns;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.util.TimeUtils;

public class RespawnOptions {
  public final Duration delay; // Minimum wait time between death and respawn
  public final long delayTicks;
  public final boolean auto; // Force dead players to respawn as soon as they can
  public final boolean blackout; // Blind dead players
  public final boolean spectate; // Allow dead players to fly around
  public final boolean bedSpawn; // Allow players to respawn from beds
  public final Filter filter; // Filter if this RespawnOption should be the one used
  public final @Nullable Component message; // Message to show respawning players, after the delay

  public RespawnOptions(
      Duration delay,
      boolean auto,
      boolean blackout,
      boolean spectate,
      boolean bedSpawn,
      Filter filter,
      @Nullable Component message) {
    this.delay = delay;
    this.delayTicks = Math.max(TimeUtils.toTicks(delay), 0);
    this.auto = auto;
    this.blackout = blackout;
    this.spectate = spectate;
    this.bedSpawn = bedSpawn;
    this.filter = filter;
    this.message = message;
  }
}
