package tc.oc.pgm.tracker.event;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.tracker.damage.SpleefInfo;

public class PlayerSpleefEvent extends Event {

  private final MatchPlayer victim;
  private final Block block;
  private final SpleefInfo info;

  public PlayerSpleefEvent(MatchPlayer victim, Block block, SpleefInfo info) {
    this.victim = checkNotNull(victim);
    this.block = checkNotNull(block);
    this.info = checkNotNull(info);
  }

  public MatchPlayer getVictim() {
    return victim;
  }

  public SpleefInfo getSpleefInfo() {
    return info;
  }

  public Block getBlock() {
    return block;
  }

  public @Nullable ParticipantState getBreaker() {
    return info.getBreaker().getAttacker();
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
