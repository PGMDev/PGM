package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapVisibility;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.text.TextFormatter;

public class MapVisibilityCommand {

  @Command(
      aliases = {"mapvisibility", "mvis"},
      desc = "Toggle viewing visibility for a map",
      perms = Permissions.MAPDEV)
  public void setVisibility(Audience viewer, CommandSender sender, MapInfo map) {
    MapVisibility visibility = PGM.get().getDatastore().getMapVisibility(map);
    visibility.setHidden(!visibility.isHidden());

    String key = visibility.isHidden() ? "hidden" : "shown";
    NamedTextColor color = visibility.isHidden() ? NamedTextColor.RED : NamedTextColor.GREEN;

    viewer.sendMessage(
        translatable(
            "map.visibility",
            NamedTextColor.GRAY,
            translatable("map.visibility." + key, color),
            map.getStyledName(MapNameStyle.COLOR)));
  }

  @Command(
      aliases = {"hiddenmaps"},
      desc = "View a list of hidden maps",
      perms = Permissions.MAPDEV)
  public void viewHiddenMaps(Audience viewer, CommandSender sender, @Default("1") int page)
      throws CommandException {
    List<MapVisibility> hiddenMaps = PGM.get().getDatastore().getHiddenMaps();

    int resultsPerPage = 8;
    int pages = (hiddenMaps.size() + resultsPerPage - 1) / resultsPerPage;

    Component paginated =
        TextFormatter.paginate(
            translatable("map.visibility.title"),
            page,
            pages,
            NamedTextColor.DARK_AQUA,
            NamedTextColor.AQUA,
            true);

    Component formattedTitle =
        TextFormatter.horizontalLineHeading(sender, paginated, NamedTextColor.DARK_AQUA, 250);

    new PrettyPaginatedComponentResults<MapVisibility>(formattedTitle, resultsPerPage) {
      @Override
      public Component format(MapVisibility map, int index) {
        return text()
            .append(text((index + 1) + ". "))
            .append(map.getMap().getStyledName(MapNameStyle.COLOR_WITH_AUTHORS))
            .clickEvent(ClickEvent.runCommand("/map " + map.getMap().getName()))
            .hoverEvent(
                HoverEvent.showText(
                    translatable(
                        "command.maps.hover",
                        NamedTextColor.GRAY,
                        map.getMap().getStyledName(MapNameStyle.COLOR))))
            .build();
      }
    }.display(viewer, hiddenMaps, page);
  }
}
