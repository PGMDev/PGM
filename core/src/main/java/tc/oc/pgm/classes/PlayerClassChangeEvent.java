package tc.oc.pgm.classes;

import static java.util.Objects.requireNonNull;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

public class PlayerClassChangeEvent extends MatchPlayerEvent {
  private final MatchPlayer player;
  private final String family;
  private final PlayerClass oldClass;
  private final PlayerClass newClass;

  public PlayerClassChangeEvent(
      MatchPlayer player, String family, PlayerClass oldClass, PlayerClass newClass) {
    super(player);
    this.player = requireNonNull(player, "player");
    this.family = requireNonNull(family, "family");
    this.oldClass = requireNonNull(oldClass, "old class");
    this.newClass = requireNonNull(newClass, "new class");
  }

  public MatchPlayer getPlayer() {
    return this.player;
  }

  public String getFamily() {
    return this.family;
  }

  public PlayerClass getOldClass() {
    return this.oldClass;
  }

  public PlayerClass getNewClass() {
    return this.newClass;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  static HandlerList handlers = new HandlerList();
}
