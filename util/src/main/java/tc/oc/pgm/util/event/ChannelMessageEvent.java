package tc.oc.pgm.util.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.channels.Channel;

public class ChannelMessageEvent extends PreemptiveEvent {

  private Channel channel;
  private @Nullable Player sender;
  private String message;

  public ChannelMessageEvent(Channel channel, Player sender, String message) {
    this.channel = channel;
    this.sender = sender;
    this.message = message;
  }

  public Channel getChannel() {
    return channel;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public Player getSender() {
    return sender;
  }

  public void setSender(Player sender) {
    this.sender = sender;
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
