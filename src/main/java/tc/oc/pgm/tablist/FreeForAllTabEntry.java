package tc.oc.pgm.tablist;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.tablist.DynamicTabEntry;
import tc.oc.tablist.TabView;
import tc.oc.util.collection.DefaultProvider;

public class FreeForAllTabEntry extends DynamicTabEntry {

  public static class Factory implements DefaultProvider<Match, FreeForAllTabEntry> {
    @Override
    public FreeForAllTabEntry get(Match key) {
      return new FreeForAllTabEntry(key);
    }
  }

  private final Match match;

  public FreeForAllTabEntry(Match match) {
    this.match = match;
  }

  @Override
  public BaseComponent getContent(TabView view) {
    return new PersonalizedText(
            new PersonalizedText(String.valueOf(match.getParticipants().size()), ChatColor.WHITE),
            new PersonalizedText("/", ChatColor.DARK_GRAY),
            new PersonalizedText(String.valueOf(match.getMaxPlayers()), ChatColor.GRAY),
            new PersonalizedText(" ", ChatColor.YELLOW, ChatColor.BOLD)
                .extra(new PersonalizedTranslatable("command.match.matchInfo.players")))
        .render(view.getViewer());
  }
}
