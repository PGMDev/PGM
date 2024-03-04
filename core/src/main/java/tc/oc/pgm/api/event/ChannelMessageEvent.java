package tc.oc.pgm.api.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.event.PreemptiveEvent;

public class ChannelMessageEvent<T> extends PreemptiveEvent {

  private final Channel<T> channel;
  private @Nullable MatchPlayer sender;
  private final T target;
  private final List<MatchPlayer> viewers;
  private String message;

  public ChannelMessageEvent(
      Channel<T> channel,
      @Nullable MatchPlayer sender,
      T target,
      Collection<MatchPlayer> viewers,
      String message) {
    this.channel = channel;
    this.sender = sender;
    this.target = target;
    this.viewers = new ArrayList<MatchPlayer>(viewers);
    this.message = message;
  }

  public Channel<T> getChannel() {
    return channel;
  }

  @Nullable
  public MatchPlayer getSender() {
    return sender;
  }

  public void setSender(@Nullable MatchPlayer sender) {
    this.sender = sender;
  }

  public T getTarget() {
    return target;
  }

  public List<MatchPlayer> getViewers() {
    return viewers;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
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
