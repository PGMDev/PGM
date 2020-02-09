package tc.oc.pgm.events;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.event.ExtendedCancellable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.commands.ModerationCommands.PunishmentType;

/** Called when a punishment command is run * */
public class PlayerPunishmentEvent extends ExtendedCancellable {

  private final CommandSender sender;
  private final MatchPlayer player;
  private final PunishmentType punishment;
  private final String reason;
  private final boolean silent;

  public PlayerPunishmentEvent(
      CommandSender sender,
      MatchPlayer player,
      PunishmentType punishment,
      String reason,
      boolean silent) {
    this.sender = checkNotNull(sender);
    this.player = checkNotNull(player);
    this.punishment = checkNotNull(punishment);
    this.reason = checkNotNull(reason);
    this.silent = checkNotNull(silent);
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

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
