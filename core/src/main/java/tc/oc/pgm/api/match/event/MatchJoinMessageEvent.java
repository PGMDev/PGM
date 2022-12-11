package tc.oc.pgm.api.match.event;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class MatchJoinMessageEvent extends MatchEvent {

  private final MatchPlayer player;

  private List<Component> extraLines;

  public MatchJoinMessageEvent(Match match, MatchPlayer player) {
    super(match);
    this.player = player;
    this.extraLines = Lists.newArrayList();
  }

  public List<Component> getExtraLines() {
    return extraLines;
  }

  public MatchPlayer getPlayer() {
    return player;
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
