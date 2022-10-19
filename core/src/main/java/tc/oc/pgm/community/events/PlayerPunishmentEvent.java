package tc.oc.pgm.community.events;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.time.Duration;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.community.command.ModerationCommand.PunishmentType;
import tc.oc.pgm.util.event.PreemptiveEvent;

/** Called when a punishment command is run * */
public class PlayerPunishmentEvent extends PreemptiveEvent {

  private final CommandSender sender;
  private final MatchPlayer player;
  private final PunishmentType punishment;
  private final String reason;
  private final Duration duration;
  private final boolean silent;

  public PlayerPunishmentEvent(
      CommandSender sender,
      MatchPlayer player,
      PunishmentType punishment,
      String reason,
      Duration duration,
      boolean silent) {
    this.sender = assertNotNull(sender);
    this.player = assertNotNull(player);
    this.punishment = assertNotNull(punishment);
    this.reason = assertNotNull(reason);
    this.duration = assertNotNull(duration);
    this.silent = assertNotNull(silent);
  }

  public CommandSender getSender() {
    return sender;
  }

  public PunishmentType getType() {
    return punishment;
  }

  public MatchPlayer getPlayer() {
    return player;
  }

  public String getReason() {
    return reason;
  }

  public boolean isSilent() {
    return silent;
  }

  public Duration getDuration() {
    return duration;
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
