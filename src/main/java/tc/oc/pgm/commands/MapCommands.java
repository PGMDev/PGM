package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import com.google.common.collect.ImmutableSortedSet;
import java.net.URL;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.command.CommandSender;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.commands.annotations.Text;
import tc.oc.pgm.map.Contributor;
import tc.oc.pgm.map.MapInfo;
import tc.oc.pgm.map.MapLibrary;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.util.PrettyPaginatedResult;
import tc.oc.util.components.Components;

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
    final Set<PGMMap> maps = ImmutableSortedSet.copyOf(library.getMaps());

    int resultsPerPage = 8;
    int pages = (library.getMaps().size() + resultsPerPage - 1) / resultsPerPage;

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

    new PrettyPaginatedResult<PGMMap>(listHeader, resultsPerPage) {
      @Override
      public String format(PGMMap map, int index) {
        return (index + 1) + ". " + map.getInfo().getShortDescription(sender);
      }
    }.display(audience, maps, page);
  }

  @Command(
      aliases = {"mapinfo", "map"},
      desc = "Shows information a certain map",
      usage = "[map name] - defaults to the current map")
  public void map(Audience audience, CommandSender sender, @Text PGMMap map) {
    MapInfo mapInfo = map.getInfo();
    audience.sendMessage(mapInfo.getFormattedMapTitle());

    Component edition =
        new PersonalizedText(
            mapInfoLabel("command.map.mapInfo.edition"),
            new PersonalizedText(mapInfo.getLocalizedEdition(), ChatColor.GOLD));

    audience.sendMessage(edition);

    audience.sendMessage(
        new PersonalizedText(
            mapInfoLabel("command.map.mapInfo.objective"),
            new PersonalizedText(mapInfo.objective, ChatColor.GOLD)));

    List<Contributor> authors = mapInfo.getNamedAuthors();
    if (authors.size() == 1) {
      audience.sendMessage(
          new PersonalizedText(
              mapInfoLabel("command.map.mapInfo.authorSingular"),
              formatContribution(authors.get(0))));
    } else {
      audience.sendMessage(mapInfoLabel("command.map.mapInfo.authorPlural"));
      for (Contributor author : authors) {
        audience.sendMessage(new PersonalizedText("  ").extra(formatContribution(author)));
      }
    }

    List<Contributor> contributors = mapInfo.getNamedContributors();
    if (!contributors.isEmpty()) {
      audience.sendMessage(mapInfoLabel("command.map.mapInfo.contributors"));
      for (Contributor contributor : contributors) {
        audience.sendMessage(new PersonalizedText("  ").extra(formatContribution(contributor)));
      }
    }

    if (mapInfo.rules.size() > 0) {
      audience.sendMessage(mapInfoLabel("command.map.mapInfo.rules"));

      for (int i = 0; i < mapInfo.rules.size(); i++) {
        audience.sendMessage(
            new PersonalizedText(
                new PersonalizedText((i + 1) + ") ", ChatColor.WHITE),
                new PersonalizedText(mapInfo.rules.get(i), ChatColor.GOLD)));
      }
    }

    audience.sendMessage(
        new PersonalizedText(
            mapInfoLabel("command.map.mapInfo.playerLimit"),
            new PersonalizedText(
                String.valueOf(map.getPersistentContext().getMaxPlayers()), ChatColor.GOLD)));

    if (sender.hasPermission(Permissions.DEBUG)) {
      audience.sendMessage(
          new PersonalizedText(
              mapInfoLabel("command.map.mapInfo.genre"),
              new PersonalizedText(mapInfo.getLocalizedGenre(), ChatColor.GOLD)));
      audience.sendMessage(
          new PersonalizedText(
              mapInfoLabel("command.map.mapInfo.proto"),
              new PersonalizedText(mapInfo.proto.toString(), ChatColor.GOLD)));
      audience.sendMessage(
          new PersonalizedText(
              mapInfoLabel("command.map.mapInfo.folder"),
              new PersonalizedText(map.getFolder().getRelativePath().toString(), ChatColor.GOLD)));
      audience.sendMessage(
          new PersonalizedText(
              mapInfoLabel("command.map.mapInfo.source"),
              new PersonalizedText(map.getSource().getPath().toString(), ChatColor.GOLD)));
    }

    URL xmlLink = map.getFolder().getDescriptionFileUrl();
    if (xmlLink != null) {
      audience.sendMessage(
          new PersonalizedText(
              new PersonalizedText(ChatColor.DARK_PURPLE, ChatColor.BOLD)
                  .extra(new PersonalizedTranslatable("command.map.mapInfo.xml"))
                  .extra(": "),
              Components.link(xmlLink)
                  .hoverEvent(
                      HoverEvent.Action.SHOW_TEXT,
                      new PersonalizedTranslatable("command.map.mapInfo.sourceCode.tip")
                          .render())));
    }
  }

  @Command(
      aliases = {"mapnext", "mn", "nextmap", "nm", "next"},
      desc = "Shows which map is coming up next")
  public void next(Audience audience, CommandSender sender, MatchManager matchManager) {
    final PGMMap next = matchManager.getMapOrder().getNextMap();

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
                    next.getInfo().getShortDescription(sender) + ChatColor.DARK_PURPLE));
  }

  private @Nullable Component formatContribution(Contributor contributor) {
    Component c = contributor.getStyledName(NameStyle.FANCY);
    if (!contributor.hasContribution()) return c;
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
