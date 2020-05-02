package tc.oc.pgm.start;

import java.time.Duration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

/** Optional countdown between teams being finalized and match starting */
public class HuddleCountdown extends PreMatchCountdown implements Listener {

  public HuddleCountdown(Match match) {
    super(match);
  }

  @Override
  protected Component formatText() {
    return new PersonalizedTranslatable("countdown.huddleEnd", secondsRemaining(ChatColor.DARK_RED))
        .color(ChatColor.YELLOW);
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
