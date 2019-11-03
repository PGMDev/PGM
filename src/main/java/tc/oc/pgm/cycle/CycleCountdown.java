package tc.oc.pgm.cycle;

import net.md_5.bungee.api.ChatColor;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.component.types.BlankComponent;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.map.PGMMap;

public class CycleCountdown extends MatchCountdown {
  protected final MatchManager mm;
  protected final PGMMap nextMap;

  public CycleCountdown(MatchManager mm, Match match, PGMMap nextMap) {
    super(match);
    this.mm = mm;
    this.nextMap = nextMap;
  }

  @Override
  protected Component formatText() {
    if (nextMap == null || nextMap.getInfo() == null) return BlankComponent.INSTANCE;
    Component mapName = new PersonalizedText(nextMap.getInfo().name, ChatColor.AQUA);

    if (remaining.isLongerThan(Duration.ZERO)) {
      return new PersonalizedText(
          new PersonalizedTranslatable(
              "countdown.cycle.message", mapName, secondsRemaining(ChatColor.DARK_RED)),
          ChatColor.DARK_AQUA);
    } else {
      return new PersonalizedText(
          new PersonalizedTranslatable("countdown.cycle.complete", mapName), ChatColor.DARK_AQUA);
    }
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);
    this.mm.cycleMatch(this.getMatch(), nextMap, false);
  }
}
