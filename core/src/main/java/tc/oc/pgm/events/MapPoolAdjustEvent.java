package tc.oc.pgm.events;

import java.time.Duration;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.rotation.pools.MapPool;

/** MapPoolAdjustEvent is called when the active {@link MapPool} is set to another * */
public class MapPoolAdjustEvent extends Event {

  private final MapPool oldPool;
  private final MapPool newPool;
  private final Match match;

  private final boolean forced;
  private final @Nullable CommandSender sender;
  private final @Nullable Duration timeLimit;
  private final int matchLimit;

  public MapPoolAdjustEvent(
      MapPool oldMapPool,
      MapPool newMapPool,
      Match match,
      boolean forced,
      @Nullable CommandSender sender,
      @Nullable Duration timeLimit,
      int matchLimit) {
    this.oldPool = oldMapPool;
    this.newPool = newMapPool;
    this.match = match;
    this.forced = forced;
    this.sender = sender;
    this.timeLimit = timeLimit;
    this.matchLimit = matchLimit;
  }

  public MapPool getOldPool() {
    return oldPool;
  }

  public MapPool getNewPool() {
    return newPool;
  }

  public Match getMatch() {
    return match;
  }

  public boolean isForced() {
    return forced;
  }

  public CommandSender getSender() {
    return sender;
  }

  public Duration getTimeLimit() {
    return timeLimit;
  }

  public int getMatchLimit() {
    return matchLimit;
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
