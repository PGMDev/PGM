package tc.oc.pgm.tablist;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.named.NameStyle;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.tablist.DynamicTabEntry;
import tc.oc.pgm.util.tablist.TabView;
import tc.oc.pgm.util.text.TextFormatter;

public class MapTabEntry extends DynamicTabEntry {

  private final MapInfo map;

  public MapTabEntry(Match match) {
    this.map = match.getMap();
  }

  @Override
  public Component getContent(TabView view) {
    MatchPlayer viewer = PGM.get().getMatchManager().getPlayer(view.getViewer());

    if (viewer != null && viewer.isLegacy()) {
      String mapName = map.getName();
      if (mapName.length() > 15) mapName = mapName.substring(0, 13) + "...";
      return text(mapName, NamedTextColor.AQUA);
    }

    return translatable(
        "misc.authorship",
        NamedTextColor.GRAY,
        text(map.getName(), NamedTextColor.AQUA, TextDecoration.BOLD),
        TextFormatter.nameList(map.getAuthors(), NameStyle.FANCY, NamedTextColor.GRAY));
  }
}
