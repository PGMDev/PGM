package tc.oc.pgm.api.player.event;

import net.kyori.adventure.text.Component;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.listeners.ChatDispatcher.Channel;

/** MatchPlayerChatEvent - Called when a match player sends a message * */
public class MatchPlayerChatEvent extends MatchPlayerEvent {

  private Component message;
  private Component sender;
  private Channel channel;

  protected MatchPlayerChatEvent(MatchPlayer player) {
    super(player);
  }

  public MatchPlayerChatEvent(MatchPlayer sender, Component message, Channel channel) {
    super(sender);
    this.message = message;
    this.sender = sender.getName();
    this.channel = channel;
  }

  public Channel getChannel() {
    return channel;
  }

  public Component getSender() {
    return sender;
  }

  public Component getMessage() {
    return message;
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
