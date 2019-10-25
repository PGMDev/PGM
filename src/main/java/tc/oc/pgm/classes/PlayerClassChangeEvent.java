package tc.oc.pgm.classes;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.events.MatchEvent;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchPlayer;

public class PlayerClassChangeEvent extends MatchEvent {
  public PlayerClassChangeEvent(
      Match match, MatchPlayer player, String family, PlayerClass oldClass, PlayerClass newClass) {
    super(match);
    this.player = checkNotNull(player, "player");
    this.family = checkNotNull(family, "family");
    this.oldClass = checkNotNull(oldClass, "old class");
    this.newClass = checkNotNull(newClass, "new class");
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

  final MatchPlayer player;
  final String family;
  final PlayerClass oldClass;
  final PlayerClass newClass;

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  static HandlerList handlers = new HandlerList();
}
