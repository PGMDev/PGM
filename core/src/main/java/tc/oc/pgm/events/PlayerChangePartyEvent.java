package tc.oc.pgm.events;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinRequest;

/**
 * Fired *around* all party changes. Use {@link this#addHandler} to attach handlers that should run
 * after the change has completed.
 */
public class PlayerChangePartyEvent extends PlayerPartyChangeEventBase {

  private final List<Runnable> postChangeHandlers = new ArrayList<>();

  public PlayerChangePartyEvent(
      MatchPlayer player, @Nullable Party oldParty, @Nullable Party newParty, JoinRequest request) {
    super(player, oldParty, newParty, request);
  }

  public void addHandler(Runnable handler) {
    this.postChangeHandlers.add(handler);
  }

  public void runHandlers() {
    postChangeHandlers.forEach(Runnable::run);
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
