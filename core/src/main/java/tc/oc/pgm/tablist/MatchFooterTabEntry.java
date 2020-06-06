package tc.oc.pgm.tablist;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.chat.BaseComponent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.modules.StatsMatchModule;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.TabView;
import tc.oc.pgm.util.text.TextTranslations;

public class MatchFooterTabEntry extends DynamicTabEntry {

  private final Match match;
  private @Nullable Future<?> tickTask;

  public MatchFooterTabEntry(Match match) {
    this.match = match;
  }

  @Override
  public void addToView(TabView view) {
    super.addToView(view);
    if (this.tickTask == null) {
      Runnable tick = MatchFooterTabEntry.this::invalidate;
      this.tickTask =
          match
              .getExecutor(MatchScope.LOADED)
              .scheduleWithFixedDelay(tick, 0, TimeUtils.TICK * 5, TimeUnit.MILLISECONDS);
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
  public BaseComponent getContent(TabView view) {
    TextComponent.Builder content = TextComponent.builder();

    MatchPlayer viewer = match.getPlayer(view.getViewer());

    if (viewer.getCompetitor() != null
        && viewer.getSettings().getValue(SettingKey.STATS).equals(SettingValue.STATS_ON)) {
      content.append(match.getModule(StatsMatchModule.class).getBasicStatsMessage(viewer.getId()));
      content.append("\n");
    }

    content
        .append(TranslatableComponent.of("match.info.time", TextColor.GRAY))
        .append(": ", TextColor.GRAY)
        .append(
            TimeUtils.formatDuration(match.getDuration()),
            this.match.isRunning() ? TextColor.GREEN : TextColor.GOLD);

    return TextTranslations.toBaseComponent(
        content.colorIfAbsent(TextColor.DARK_GRAY).build(), view.getViewer());
  }
}
