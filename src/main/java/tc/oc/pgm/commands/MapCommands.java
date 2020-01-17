package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapInfoExtra;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.commands.annotations.Text;
import tc.oc.pgm.rotation.MapOrder;
import tc.oc.pgm.util.PrettyPaginatedResult;
import tc.oc.pgm.util.TranslationUtils;
import tc.oc.util.components.ComponentUtils;

public class MapCommands {

  @Command(
      aliases = {"maplist", "maps", "ml"},
      desc = "Shows the maps that are currently loaded",
      usage = "[page]",
      help =
          "Shows all the maps that are currently loaded including ones that are not in the rotation.")
  public static void maplist(
      Audience audience, CommandSender sender, MapLibrary library, @Default("1") int page)
      throws CommandException {
    final Set<MapInfo> maps = ImmutableSortedSet.copyOf(library.getMaps());

    int resultsPerPage = 8;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    String listHeader =
        ChatColor.BLUE.toString()
            + ChatColor.STRIKETHROUGH
            + "---------------"
            + ChatColor.RESET
            + " "
            + AllTranslations.get().translate("command.map.mapList.title", sender)
            + ChatColor.DARK_AQUA
            + " ("
            + ChatColor.AQUA
            + page
            + ChatColor.DARK_AQUA
            + " of "
            + ChatColor.AQUA
            + pages
            + ChatColor.DARK_AQUA
            + ") "
            + ChatColor.BLUE.toString()
            + ChatColor.STRIKETHROUGH
            + " ---------------"
            + ChatColor.RESET;

    new PrettyPaginatedResult<MapInfo>(listHeader, resultsPerPage) {
      @Override
      public String format(MapInfo map, int index) {
        // TODO: fix misc.authorship
        return (index + 1)
            + ". "
            + ChatColor.RED
            + map.getName()
            + " "
            + ChatColor.DARK_PURPLE
            + TranslationUtils.nameList(NameStyle.FANCY, map.getAuthors())
                .render(sender)
                .toLegacyText();
      }
    }.display(audience, maps, page);
  }

  @Command(
      aliases = {"mapinfo", "map"},
      desc = "Shows information a certain map",
      usage = "[map name] - defaults to the current map")
  public void map(Audience audience, CommandSender sender, @Text MapInfo map) {
    audience.sendMessage(ComponentUtils.horizontalLineHeading(map.getName(), ChatColor.RED, 200));

    audience.sendMessage(
        new PersonalizedText(
            mapInfoLabel("command.map.mapInfo.objective"),
            new PersonalizedText(map.getDescription(), ChatColor.GOLD)));

    Collection<Contributor> authors = map.getAuthors();
    if (authors.size() == 1) {
      audience.sendMessage(
          new PersonalizedText(
              mapInfoLabel("command.map.mapInfo.authorSingular"),
              formatContribution(authors.iterator().next())));
    } else {
      audience.sendMessage(mapInfoLabel("command.map.mapInfo.authorPlural"));
      for (Contributor author : authors) {
        audience.sendMessage(new PersonalizedText("  ").extra(formatContribution(author)));
      }
    }

    Collection<Contributor> contributors = map.getContributors();
    if (!contributors.isEmpty()) {
      audience.sendMessage(mapInfoLabel("command.map.mapInfo.contributors"));
      for (Contributor contributor : contributors) {
        audience.sendMessage(new PersonalizedText("  ").extra(formatContribution(contributor)));
      }
    }

    if (map.getRules().size() > 0) {
      audience.sendMessage(mapInfoLabel("command.map.mapInfo.rules"));

      int i = 0;
      for (String rule : map.getRules()) {
        audience.sendMessage(
            new PersonalizedText(
                new PersonalizedText(++i + ") ", ChatColor.WHITE),
                new PersonalizedText(rule, ChatColor.GOLD)));
      }
    }

    if (map instanceof MapInfoExtra) {
      audience.sendMessage(
          new PersonalizedText(
              mapInfoLabel("command.map.mapInfo.playerLimit"),
              new PersonalizedText(
                  String.valueOf(((MapInfoExtra) map).getPlayerLimit()), ChatColor.GOLD)));
    }

    if (sender.hasPermission(Permissions.DEBUG)) {
      if (map instanceof MapInfoExtra) {
        audience.sendMessage(
            new PersonalizedText(
                mapInfoLabel("command.map.mapInfo.genre"),
                new PersonalizedText(((MapInfoExtra) map).getGenre(), ChatColor.GOLD)));
      }
      audience.sendMessage(
          new PersonalizedText(
              mapInfoLabel("command.map.mapInfo.proto"),
              new PersonalizedText(map.getProto().toString(), ChatColor.GOLD)));
    }
  }

  @Command(
      aliases = {"mapnext", "mn", "nextmap", "nm", "next"},
      desc = "Shows which map is coming up next")
  public void next(Audience audience, CommandSender sender, MapOrder mapOrder) {
    final MapInfo next = mapOrder.getNextMap();

    if (next == null) {
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.map.next.noNextMap", sender));
      return;
    }

    audience.sendMessage(
        ChatColor.DARK_PURPLE
            + AllTranslations.get()
                .translate(
                    "command.map.next.success",
                    sender,
                    next.getDescription() + ChatColor.DARK_PURPLE));
  }

  private @Nullable Component formatContribution(Contributor contributor) {
    Component c = contributor.getStyledName(NameStyle.FANCY);
    if (contributor.getContribution() == null) return c;
    return new PersonalizedText(
        c,
        new PersonalizedText(ChatColor.GRAY, ChatColor.ITALIC)
            .extra(" - ")
            .extra(contributor.getContribution()));
  }

  private Component mapInfoLabel(String key) {
    return new PersonalizedText(
            new PersonalizedTranslatable(key), ChatColor.DARK_PURPLE, ChatColor.BOLD)
        .extra(": ");
  }
}
