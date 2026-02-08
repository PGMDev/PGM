package tc.oc.pgm.api.event;

import static net.kyori.adventure.text.Component.text;

import java.util.Collection;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.event.PreemptiveEvent;

public class ChannelMessageEvent<T> extends PreemptiveEvent {

  @Getter
  private final Channel<T> channel;

  @Getter
  private final MatchPlayer sender;

  @Getter
  private final T target;

  @Getter
  @Setter
  private Collection<MatchPlayer> viewers;

  @Getter
  private String message;

  @Setter
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

  public void setMessage(String message) {
    this.message = message;
    this.component = null;
  }

  public Component getComponent() {
    return (component != null) ? component : text(message);
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
