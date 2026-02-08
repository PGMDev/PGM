package tc.oc.pgm.classes;

import static tc.oc.pgm.util.Assert.assertNotNull;

import lombok.Getter;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

@Getter
public class PlayerClassChangeEvent extends MatchPlayerEvent {
  private final MatchPlayer player;
  private final String family;
  private final PlayerClass oldClass;
  private final PlayerClass newClass;

  public PlayerClassChangeEvent(
      MatchPlayer player, String family, PlayerClass oldClass, PlayerClass newClass) {
    super(player);
    this.player = assertNotNull(player, "player");
    this.family = assertNotNull(family, "family");
    this.oldClass = assertNotNull(oldClass, "old class");
    this.newClass = assertNotNull(newClass, "new class");
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
