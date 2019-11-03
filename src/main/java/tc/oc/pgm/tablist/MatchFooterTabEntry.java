package tc.oc.pgm.tablist;

import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.tablist.DynamicTabEntry;
import tc.oc.tablist.TabView;
import tc.oc.util.collection.DefaultProvider;
import tc.oc.util.components.PeriodFormats;

public class MatchFooterTabEntry extends DynamicTabEntry {

  public static class Factory implements DefaultProvider<Match, MatchFooterTabEntry> {
    @Override
    public MatchFooterTabEntry get(Match key) {
      return new MatchFooterTabEntry(key);
    }
  }

  private final Match match;
  private @Nullable BukkitTask tickTask;

  public MatchFooterTabEntry(Match match) {
    this.match = match;
  }

  @Override
  public void addToView(TabView view) {
    super.addToView(view);
    if (this.tickTask == null) {
      Runnable tick = MatchFooterTabEntry.this::invalidate;
      this.tickTask = match.getScheduler(MatchScope.LOADED).runTaskTimer(5, 20, tick);
    }
  }

  @Override
  public void removeFromView(TabView view) {
    super.removeFromView(view);
    if (!this.hasViews() && this.tickTask != null) {
      this.tickTask.cancel();
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
            PeriodFormats.COLONS.print(this.match.getDuration().toPeriod()),
            this.match.isRunning() ? ChatColor.GREEN : ChatColor.GOLD));

    if (server != null) {
      content.extra(
          new PersonalizedText(" - "),
          new PersonalizedText(server, ChatColor.WHITE, ChatColor.BOLD));
    }

    return content.render(view.getViewer());
  }
}
