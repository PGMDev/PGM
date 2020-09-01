package tc.oc.pgm.tablist;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import net.md_5.bungee.api.chat.BaseComponent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.TabView;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public class MapTabEntry extends DynamicTabEntry {

  private final MapInfo map;

  public MapTabEntry(Match match) {
    this.map = match.getMap();
  }

  @Override
  public BaseComponent[] getContent(TabView view) {
    MatchPlayer viewer = PGM.get().getMatchManager().getPlayer(view.getViewer());

    if (viewer != null && viewer.isLegacy()) {
      String mapName = map.getName();
      if (mapName.length() > 15) mapName = mapName.substring(0, 13) + "...";
      return TextTranslations.toBaseComponentArray(
          TextComponent.of(mapName, TextColor.AQUA), view.getViewer());
    }

    final Component text =
        TranslatableComponent.of(
            "misc.authorship",
            TextColor.GRAY,
            TextComponent.of(map.getName(), TextColor.AQUA, TextDecoration.BOLD),
            TextFormatter.nameList(map.getAuthors(), NameStyle.FANCY, TextColor.GRAY));

    return TextTranslations.toBaseComponentArray(text, view.getViewer());
  }
}
