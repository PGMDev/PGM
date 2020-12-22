package tc.oc.pgm.start;

import static net.kyori.adventure.text.Component.translatable;

import java.time.Duration;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.join.JoinMatchModule;

/** Optional countdown between teams being finalized and match starting */
public class HuddleCountdown extends PreMatchCountdown implements Listener {

  public HuddleCountdown(Match match) {
    super(match, BossBar.Color.YELLOW);
  }

  @Override
  protected Component formatText() {
    return translatable(
        "countdown.huddleEnd", NamedTextColor.YELLOW, secondsRemaining(NamedTextColor.DARK_RED));
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
