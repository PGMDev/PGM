package tc.oc.pgm.loot;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.CountdownContext;
import tc.oc.pgm.countdowns.MatchCountdown;

public class LootCountdown extends MatchCountdown implements Comparable<LootCountdown> {
  private final CountdownContext context;
  private final LootableDefinition lootableDefinition;

  public LootCountdown(Match match, LootMatchModule parent, LootableDefinition lootableDefinition) {
    super(match);
    this.context = parent.getCountdown();
    this.lootableDefinition = lootableDefinition;
  }

  public CountdownContext getContext() {
    return context;
  }

  public LootableDefinition getLootableDefinition() {
    return lootableDefinition;
  }

  @Override
  protected Component formatText() {
    return null;
  }

  // not applicable
  public boolean showBossBar() {
    return false;
  }

  // not something useful
  @Override
  public int compareTo(@NotNull LootCountdown lootCountdown) {
    return 0;
  }
}
