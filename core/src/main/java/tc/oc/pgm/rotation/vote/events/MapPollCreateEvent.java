package tc.oc.pgm.rotation.vote.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.rotation.vote.MapPoll;

public class MapPollCreateEvent extends Event {

  private final MapPoll poll;

  public MapPollCreateEvent(MapPoll poll) {
    this.poll = poll;
  }

  public MapPoll getPoll() {
    return poll;
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
