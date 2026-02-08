package tc.oc.pgm.api.event;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.tracker.info.SpleefInfo;

@RequiredArgsConstructor
public class PlayerSpleefEvent extends Event {

  @Getter
  private final @NonNull MatchPlayer victim;

  @Getter
  private final @NonNull Vector block;

  @Getter
  private final @NonNull SpleefInfo spleefInfo;

  public @Nullable ParticipantState getBreaker() {
    return spleefInfo.getBreaker().getAttacker();
  }

  public static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
