package tc.oc.pgm.start;

import java.time.Duration;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.join.JoinMatchModule;

/** Optional countdown between teams being finalized and match starting */
public class HuddleCountdown extends PreMatchCountdown implements Listener {

  public HuddleCountdown(Match match) {
    super(match);
  }

  @Override
  protected Component formatText() {
    return TranslatableComponent.of(
        "countdown.huddleEnd", TextColor.YELLOW, secondsRemaining(TextColor.DARK_RED));
  }

  @Override
  public void onStart(Duration remaining, Duration total) {
    super.onStart(remaining, total);

    getMatch().addListener(this, MatchScope.LOADED);

    JoinMatchModule jmm = getMatch().needModule(JoinMatchModule.class);
    jmm.queuedJoin(jmm.getQueuedParticipants());
  }

  protected void cleanup() {
    HandlerList.unregisterAll(this);
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);
    cleanup();
    getMatch().start();
  }

  @Override
  public void onCancel(Duration remaining, Duration total) {
    super.onCancel(remaining, total);
    cleanup();
  }
}
