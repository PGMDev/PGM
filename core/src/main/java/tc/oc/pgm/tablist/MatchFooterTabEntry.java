package tc.oc.pgm.tablist;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.util.TimeUtils;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedText;
import tc.oc.util.bukkit.tablist.DynamicTabEntry;
import tc.oc.util.bukkit.tablist.TabView;
import tc.oc.util.bukkit.translations.AllTranslations;

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
    Component content = new PersonalizedText(ChatColor.DARK_GRAY);

    String datacenter = Config.PlayerList.datacenter();
    String server = Config.PlayerList.server();

    if (datacenter != null) {
      content.extra(
          new PersonalizedText(datacenter, ChatColor.WHITE, ChatColor.BOLD),
          new PersonalizedText(" - "));
    }

    content.extra(
        new PersonalizedText(
            AllTranslations.get().translate("command.match.matchInfo.time", view.getViewer())
                + ": ",
            ChatColor.GRAY),
        new PersonalizedText(
            TimeUtils.formatDuration(match.getDuration()),
            this.match.isRunning() ? ChatColor.GREEN : ChatColor.GOLD));

    if (server != null) {
      content.extra(
          new PersonalizedText(" - "),
          new PersonalizedText(server, ChatColor.WHITE, ChatColor.BOLD));
    }

    return content.render(view.getViewer());
  }
}
