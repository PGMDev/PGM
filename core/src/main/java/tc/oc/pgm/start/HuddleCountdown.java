package tc.oc.pgm.start;

import java.time.Duration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.PeriodFormats;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

/** Optional countdown between teams being finalized and match starting */
public class HuddleCountdown extends PreMatchCountdown implements Listener {

  public HuddleCountdown(Match match) {
    super(match);
  }

  @Override
  protected Component formatText() {
    return new PersonalizedTranslatable(
            "countdown.huddle.message", secondsRemaining(ChatColor.DARK_RED))
        .color(ChatColor.YELLOW);
  }

  @Override
  public void onStart(Duration remaining, Duration total) {
    super.onStart(remaining, total);

    getMatch().addListener(this, MatchScope.LOADED);

    for (Competitor competitor : getMatch().getCompetitors()) {
      if (competitor instanceof Team) {
        competitor.sendMessage(
            new PersonalizedText(
                new PersonalizedTranslatable(
                    "huddle.instructions", PeriodFormats.briefNaturalPrecise(total)),
                ChatColor.YELLOW));
      }
    }
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
