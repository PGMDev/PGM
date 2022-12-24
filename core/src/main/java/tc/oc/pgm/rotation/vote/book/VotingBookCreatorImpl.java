package tc.oc.pgm.rotation.vote.book;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.player.MatchPlayer;

public class VotingBookCreatorImpl implements VotingBookCreator {

  private static final String SYMBOL_IGNORE = "\u2715"; // ✕
  private static final String SYMBOL_VOTED = "\u2714"; // ✔

  @Override
  public Component getMapBookComponent(MatchPlayer viewer, MapInfo map, boolean voted) {
    TextComponent.Builder text = text();
    text.append(
        text(
            voted ? SYMBOL_VOTED : SYMBOL_IGNORE,
            voted ? NamedTextColor.DARK_GREEN : NamedTextColor.DARK_RED));
    text.append(text(" ").decoration(TextDecoration.BOLD, !voted)); // Fix 1px symbol diff
    text.append(text(map.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));
    text.hoverEvent(
        showText(
            text(
                map.getTags().stream().map(MapTag::toString).collect(Collectors.joining(" ")),
                NamedTextColor.YELLOW)));
    text.clickEvent(runCommand("/votenext -o " + map.getName()));
    return text.build();
  }
}
