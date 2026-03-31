package tc.oc.pgm.api.event;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.tracker.info.SpleefInfo;

public class PlayerSpleefEvent extends Event {

  private final MatchPlayer victim;
  private final Vector block;
  private final SpleefInfo spleefInfo;

  public PlayerSpleefEvent(MatchPlayer victim, Vector block, SpleefInfo spleefInfo) {
    this.victim = assertNotNull(victim);
    this.block = assertNotNull(block);
    this.spleefInfo = assertNotNull(spleefInfo);
  }

  public MatchPlayer victim() {
    return victim;
  }

  @Deprecated
  public MatchPlayer getVictim() {
    return victim();
  }

  public SpleefInfo spleefInfo() {
    return spleefInfo;
  }

  @Deprecated
  public SpleefInfo getSpleefInfo() {
    return spleefInfo();
  }

  public Vector block() {
    return block;
  }

  @Deprecated
  public Vector getBlock() {
    return block();
  }

  public @Nullable ParticipantState breaker() {
    return spleefInfo.breaker().attacker();
  }

  @Deprecated
  public @Nullable ParticipantState getBreaker() {
    return breaker();
  }

  // Bukkit event junk
  public static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
