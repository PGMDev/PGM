package tc.oc.pgm.events;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.Nullable;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

/**
 * Called AFTER a player leaves and/or joins a party. Subclasses are called for more specific cases,
 * and those should be used whenever possible.
 *
 * <p>If the player is leaving the match, they will have already been removed when this event is
 * called, and their party will be set to null.
 */
public class PlayerPartyChangeEvent extends MatchPlayerEvent {

  protected final @Nullable Party oldParty;
  protected final @Nullable Party newParty;
  protected boolean cancelled;

  public PlayerPartyChangeEvent(
      MatchPlayer player, @Nullable Party oldParty, @Nullable Party newParty) {
    super(player);
    checkArgument(oldParty != newParty);
    this.oldParty = oldParty;
    this.newParty = newParty;
  }

  public @Nullable Party getOldParty() {
    return this.oldParty;
  }

  public @Nullable Party getNewParty() {
    return this.newParty;
  }

  public boolean wasParticipating() {
    return oldParty != null && oldParty.isParticipating();
  }

  public boolean isParticipating() {
    return newParty != null && newParty.isParticipating();
  }

  private static final HandlerList handlers = new HandlerList();

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
