package tc.oc.pgm.broadcast;

import java.time.Duration;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.countdowns.MatchCountdown;

public class BroadcastCountdown extends MatchCountdown {
  private final Broadcast broadcast;

  public BroadcastCountdown(Match match, Broadcast broadcast) {
    super(match);
    this.broadcast = broadcast;
  }

  @Override
  protected boolean showBossBar() {
    return false;
  }

  @Override
  protected boolean showChat() {
    return false;
  }

  @Override
  protected Component formatText() {
    return TextComponent.empty();
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);
    for (MatchPlayer player : this.getMatch().getPlayers()) {
      if (this.broadcast.filter == null
          || this.broadcast.filter.query(player.getQuery()).isAllowed()) {
        player.sendMessage(this.broadcast.getFormattedMessage());
        player.playSound(this.broadcast.getSound());
      }
    }
  }
}
