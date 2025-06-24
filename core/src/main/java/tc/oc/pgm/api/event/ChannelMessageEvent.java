package tc.oc.pgm.api.event;

import static net.kyori.adventure.text.Component.text;

import java.util.Collection;
import net.kyori.adventure.text.Component;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.event.PreemptiveEvent;

public class ChannelMessageEvent<T> extends PreemptiveEvent {

  private final Channel<T> channel;
  private final MatchPlayer sender;
  private final T target;
  private Collection<MatchPlayer> viewers;
  private String message;
  private Component component;

  public ChannelMessageEvent(
      Channel<T> channel,
      MatchPlayer sender,
      T target,
      Collection<MatchPlayer> viewers,
      String message) {
    this.channel = channel;
    this.sender = sender;
    this.target = target;
    this.viewers = viewers;
    this.message = message;
  }

  public Channel<T> getChannel() {
    return channel;
  }

  public MatchPlayer getSender() {
    return sender;
  }

  public T getTarget() {
    return target;
  }

  public Collection<MatchPlayer> getViewers() {
    return viewers;
  }

  public void setViewers(Collection<MatchPlayer> viewers) {
    this.viewers = viewers;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
    this.component = null;
  }

  public Component getComponent() {
    return (component != null) ? component : text(message);
  }

  public void setComponent(Component component) {
    this.component = component;
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
