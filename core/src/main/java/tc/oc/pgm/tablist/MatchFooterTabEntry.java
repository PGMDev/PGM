package tc.oc.pgm.tablist;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TemporalComponent.clock;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.TabView;

public class MatchFooterTabEntry extends DynamicTabEntry {

  private final Match match;
  private @Nullable Future<?> tickTask;

  public MatchFooterTabEntry(Match match) {
    this.match = match;
  }

  @Override
  public void addToView(TabView view) {
    super.addToView(view);
    if (this.tickTask == null && match.isLoaded()) {
      Runnable tick = MatchFooterTabEntry.this::invalidate;
      this.tickTask =
          match.getExecutor(MatchScope.LOADED).scheduleWithFixedDelay(tick, 0, 1, TimeUnit.SECONDS);
    }
  }

  @Override
  public void removeFromView(TabView view) {
    super.removeFromView(view);
    if (!this.hasViews() && this.tickTask != null) {
      this.tickTask.cancel(true);
      this.tickTask = null;
    }
  }

  @Override
  public Component getContent(TabView view) {
    TextComponent.Builder content = text();

    MatchPlayer viewer = match.getPlayer(view.getViewer());
    boolean timeOnly = viewer != null && viewer.isLegacy();

    if (!timeOnly
        && viewer != null
        && viewer.getParty() instanceof Competitor
        && (match.isRunning() || match.isFinished())
        && viewer.getSettings().getValue(SettingKey.STATS).equals(SettingValue.STATS_ON)) {
      content.append(match.needModule(StatsMatchModule.class).getBasicStatsMessage(viewer.getId()));
      content.append(newline());
    }

    final Component leftContent = PGM.get().getConfiguration().getLeftTablistText();
    final Component rightContent = PGM.get().getConfiguration().getRightTablistText();

    if (!timeOnly && leftContent != null) {
      content
          .append(leftContent.colorIfAbsent(NamedTextColor.WHITE))
          .append(text(" - ", NamedTextColor.DARK_GRAY));
    }

    content
        .append(translatable("match.info.time", NamedTextColor.GRAY))
        .append(text(": ", NamedTextColor.GRAY))
        .append(
            clock(match.getDuration())
                .color(this.match.isRunning() ? NamedTextColor.GREEN : NamedTextColor.GOLD));

    if (!timeOnly && rightContent != null) {
      content
          .append(text(" - ", NamedTextColor.DARK_GRAY))
          .append(rightContent.colorIfAbsent(NamedTextColor.WHITE));
    }

    return content.colorIfAbsent(NamedTextColor.DARK_GRAY).build();
  }
}
