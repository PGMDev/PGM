package tc.oc.pgm.commands;

import static com.google.common.base.Preconditions.checkNotNull;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Switch;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import java.util.Collection;
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
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.commands.annotations.Text;
import tc.oc.pgm.rotation.MapOrder;
import tc.oc.pgm.util.PrettyPaginatedResult;
import tc.oc.util.components.ComponentUtils;
import tc.oc.util.components.Components;

public class MapCommands {

  @Command(
      aliases = {"loadnewmaps"},
      desc = "Loads new maps and outputs any errors")
  public static void loadNewMaps(MapLibrary library, @Switch('f') boolean force) {
    library.loadNewMaps(force); // MapLibrary will handle sending output asynchronously
  }

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
      @Fallback(Type.NULL) @Switch('a') String author,
      @Fallback(Type.NULL) @Switch('p') Integer page)
      throws CommandException {
    if (page == null) page = 1;

    Stream<MapInfo> search = Sets.newHashSet(library.getMaps()).stream();
    // FIXME: Add tag support again
    if (author != null) {
      search = search.filter(map -> matchesAuthor(map, author));
    }

    Set<MapInfo> maps = search.collect(Collectors.toCollection(TreeSet::new));
    int resultsPerPage = 8;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    String title =
        ComponentUtils.paginate(
            AllTranslations.get().translate("command.map.mapList.title", sender), page, pages);
    String listHeader =
        ComponentUtils.horizontalLineHeading(title, ChatColor.BLUE, ComponentUtils.MAX_CHAT_WIDTH);

    new PrettyPaginatedResult<MapInfo>(listHeader, resultsPerPage) {
      @Override
      public String format(MapInfo map, int index) {
        return (index + 1) + ". " + map.getStyledName(NameStyle.FANCY).toLegacyText();
      }
    }.display(audience, ImmutableSortedSet.copyOf(maps), page);
  }

  private static boolean matchesAuthor(MapInfo map, String query) {
    checkNotNull(map);
    query = checkNotNull(query).toLowerCase();

    for (Contributor contributor : map.getAuthors()) {
      if (contributor.getName().toLowerCase().contains(query)) {
        return true;
      }
    }
    return false;
  }

  @Command(
      aliases = {"mapinfo", "map"},
      desc = "Shows information a certain map",
      usage = "[map name] - defaults to the current map")
  public void map(Audience audience, CommandSender sender, @Text MapInfo map) {
    audience.sendMessage(
        ComponentUtils.horizontalLineHeading(
            ChatColor.DARK_AQUA
                + map.getName()
                + " "
                + ChatColor.GRAY
                + map.getVersion().toString(),
            ChatColor.RED));

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

    if (!map.getRules().isEmpty()) {
      audience.sendMessage(mapInfoLabel("command.map.mapInfo.rules"));

      int i = 0;
      for (String rule : map.getRules()) {
        audience.sendMessage(
            new PersonalizedText(
                new PersonalizedText(++i + ") ", ChatColor.WHITE),
                new PersonalizedText(rule, ChatColor.GOLD)));
      }
    }

    audience.sendMessage(
        new PersonalizedText(
            mapInfoLabel("command.map.mapInfo.playerLimit"),
            createPlayerLimitComponent(sender, map)));

    if (sender.hasPermission(Permissions.DEBUG)) {
      audience.sendMessage(
          new PersonalizedText(
              mapInfoLabel("command.map.mapInfo.proto"),
              new PersonalizedText(map.getProto().toString(), ChatColor.GOLD)));
    }

    audience.sendMessage(createTagsComponent(map.getTags()));
  }

  private Component createTagsComponent(Collection<MapTag> tags) {
    checkNotNull(tags);

    Component result = mapInfoLabel("command.map.mapInfo.tags");
    MapTag[] mapTags = tags.toArray(new MapTag[0]);
    for (int i = 0; i < mapTags.length; i++) {
      if (i != 0) {
        result.extra(Components.space());
      }

      String mapTag = mapTags[i].toString();
      Component component =
          new PersonalizedText(mapTag, ChatColor.GOLD)
              .bold(false)
              .clickEvent(ClickEvent.Action.RUN_COMMAND, "/maps " + mapTag)
              .hoverEvent(
                  HoverEvent.Action.SHOW_TEXT,
                  new PersonalizedTranslatable("command.map.mapTag.hover", mapTag).render());
      result.extra(component);
    }
    return result;
  }

  private static Component createPlayerLimitComponent(CommandSender sender, MapInfo map) {
    checkNotNull(sender);
    checkNotNull(map);

    Collection<Integer> maxPlayers = map.getMaxPlayers();
    if (maxPlayers.isEmpty()) {
      return Components.blank();
    } else if (maxPlayers.size() == 1) {
      return new PersonalizedText(maxPlayers.iterator().next().toString(), ChatColor.GOLD);
    }

    int totalPlayers = maxPlayers.stream().mapToInt(i -> i).sum();
    Component total = new PersonalizedText(Integer.toString(totalPlayers), ChatColor.GOLD);

    String verboseVs =
        " " + AllTranslations.get().translate("command.map.mapInfo.playerLimit.vs", sender) + " ";
    Component verbose =
        new PersonalizedText(
            new PersonalizedText("(")
                .extra(
                    maxPlayers.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(verboseVs)))
                .extra(")"),
            ChatColor.GRAY);

    return total.extra(" ").extra(verbose);
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
                    next.getStyledName(NameStyle.FANCY).toLegacyText() + ChatColor.DARK_PURPLE));
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
