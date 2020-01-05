package tc.oc.pgm.commands;

import static com.google.common.base.Preconditions.*;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Switch;
import com.google.common.collect.ImmutableSortedSet;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
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
import tc.oc.pgm.map.MapPersistentContext;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.maptag.MapTagsCondition;
import tc.oc.pgm.util.PrettyPaginatedResult;
import tc.oc.util.components.ComponentUtils;
import tc.oc.util.components.Components;

public class MapCommands {

  @Command(
      aliases = {"maplist", "maps", "ml"},
      desc = "Shows the maps that are currently loaded",
      usage = "[-a <author>] [-p <page>] [[!]#<maptag>...]",
      help =
          "Shows all the maps that are currently loaded including ones that are not in the rotation.")
  public static void maplist(
      Audience audience,
      CommandSender sender,
      MapLibrary library,
      MapTagsCondition mapTags,
      @Fallback(Type.NULL) @Switch('a') String author,
      @Fallback(Type.NULL) @Switch('p') Integer page)
      throws CommandException {
    if (page == null) page = 1;

    Stream<PGMMap> search = library.getMaps().stream().filter(mapTags);
    if (author != null) {
      search = search.filter(map -> matchesAuthor(map, author));
    }

    Set<PGMMap> maps = search.collect(Collectors.toCollection(TreeSet::new));
    int resultsPerPage = 8;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    String title =
        ComponentUtils.paginate(
            AllTranslations.get().translate("command.map.mapList.title", sender), page, pages);
    String listHeader =
        ComponentUtils.horizontalLineHeading(title, ChatColor.BLUE, ComponentUtils.MAX_CHAT_WIDTH);

    new PrettyPaginatedResult<PGMMap>(listHeader, resultsPerPage) {
      @Override
      public String format(PGMMap map, int index) {
        return (index + 1) + ". " + map.getInfo().getShortDescription(sender);
      }
    }.display(audience, ImmutableSortedSet.copyOf(maps), page);
  }

  private static boolean matchesAuthor(PGMMap map, String query) {
    checkNotNull(map);
    query = checkNotNull(query).toLowerCase(Locale.ROOT);

    for (Contributor contributor : map.getInfo().getNamedAuthors()) {
      if (contributor.getName().toLowerCase(Locale.ROOT).contains(query)) {
        return true;
      }
    }
    return false;
  }

  @Command(
      aliases = {"mapinfo", "map"},
      desc = "Shows information a certain map",
      usage = "[map name] - defaults to the current map")
  public void map(Audience audience, CommandSender sender, @Text PGMMap map) {
    MapInfo mapInfo = map.getInfo();
    audience.sendMessage(mapInfo.getFormattedMapTitle());

    MapPersistentContext persistentContext = map.getPersistentContext();
    Set<MapTag> mapTags = persistentContext.getMapTags();
    audience.sendMessage(createTagsComponent(mapTags).color(ChatColor.DARK_AQUA));

    Component edition = new PersonalizedText(mapInfo.getLocalizedEdition(), ChatColor.GOLD);
    if (!edition.toPlainText().isEmpty()) {
      audience.sendMessage(
          new PersonalizedText(mapInfoLabel("command.map.mapInfo.edition"), edition));
    }

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
            createPlayerLimitComponent(sender, persistentContext)));

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

  private static Component createTagsComponent(Set<MapTag> tags) {
    checkNotNull(tags);

    Component result = new PersonalizedText();
    MapTag[] mapTags = tags.toArray(new MapTag[0]);
    for (int i = 0; i < mapTags.length; i++) {
      if (i != 0) {
        result.extra(Components.space());
      }

      MapTag mapTag = mapTags[i];
      Component component =
          mapTags[i]
              .getComponentName()
              .clickEvent(ClickEvent.Action.RUN_COMMAND, "/maplist " + mapTag.toString())
              .hoverEvent(
                  HoverEvent.Action.SHOW_TEXT,
                  new PersonalizedTranslatable("command.map.mapTag.hover", mapTag.toString())
                      .render());
      result.extra(component);
    }
    return result;
  }

  private static Component createPlayerLimitComponent(
      CommandSender sender, MapPersistentContext persistentContext) {
    checkNotNull(sender);
    checkNotNull(persistentContext);

    List<Integer> maxPlayers = persistentContext.getMaxPlayers();
    if (maxPlayers.isEmpty()) {
      return Components.blank();
    } else if (maxPlayers.size() == 1) {
      return new PersonalizedText(maxPlayers.get(0).toString(), ChatColor.GOLD);
    }

    Component total =
        new PersonalizedText(
            Integer.toString(persistentContext.getTotalMaxPlayers()), ChatColor.GOLD);

    String verboseVs =
        " " + AllTranslations.get().translate("command.map.mapInfo.playerLimit.vs", sender) + " ";
    Component verbose =
        new PersonalizedText(
            new PersonalizedText("(")
                .extra(
                    persistentContext.getMaxPlayers().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(verboseVs)))
                .extra(")"),
            ChatColor.GRAY);

    return total.extra(" ").extra(verbose);
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
