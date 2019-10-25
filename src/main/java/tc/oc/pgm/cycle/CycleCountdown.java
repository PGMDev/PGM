package tc.oc.pgm.cycle;

import net.md_5.bungee.api.ChatColor;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.component.types.BlankComponent;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchManager;

public class CycleCountdown extends MatchCountdown {
  protected final MatchManager mm;

  public CycleCountdown(MatchManager mm, Match match) {
    super(match);
    this.mm = mm;
  }

  @Override
  protected Component formatText() {
    PGMMap nextMap = mm.getNextMap();
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
    this.mm.cycle(this.getMatch(), true, false);
  }
}
