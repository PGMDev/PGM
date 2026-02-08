package tc.oc.pgm.events;

import java.time.Duration;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.rotation.pools.MapPool;

/** MapPoolAdjustEvent is called when the active {@link MapPool} is set to another * */
public class MapPoolAdjustEvent extends Event {

  @Getter
  private final MapPool oldPool;

  @Getter
  private final MapPool newPool;

  @Getter
  private final Match match;

  @Getter
  private final boolean forced;

  private final @Nullable CommandSender sender;
  private final @Nullable Duration timeLimit;

  @Getter
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

  public @Nullable CommandSender getSender() {
    return sender;
  }

  public @Nullable Duration getTimeLimit() {
    return timeLimit;
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
