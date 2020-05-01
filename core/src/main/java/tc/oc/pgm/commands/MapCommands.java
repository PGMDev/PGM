package tc.oc.pgm.commands;

import static com.google.common.base.Preconditions.checkNotNull;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.commands.annotations.Text;
import tc.oc.pgm.rotation.MapPool;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.util.PrettyPaginatedResult;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentUtils;
import tc.oc.pgm.util.component.Components;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextTranslations;

public class MapCommands {

  @Command(
      aliases = {"loadnewmaps"},
      desc = "Loads new maps and outputs any errors")
  public static void loadNewMaps(MapLibrary library, @Switch('f') boolean force) {
    library.loadNewMaps(force); // MapLibrary will handle sending output asynchronously
  }

  @Command(
      aliases = {"maps", "maplist", "ml"},
      desc = "Shows the maps that are currently loaded",
      usage = "[-a <author>] [-t <tag1>,<tag2>]",
      help =
          "Shows all the maps that are currently loaded including ones that are not in the rotation.")
  public static void maplist(
      Audience audience,
      CommandSender sender,
      MapLibrary library,
      @Default("1") Integer page,
      @Fallback(Type.NULL) @Switch('t') String tags,
      @Fallback(Type.NULL) @Switch('a') String author)
      throws CommandException {
    Stream<MapInfo> search = Sets.newHashSet(library.getMaps()).stream();
    if (tags != null) {
      final Map<Boolean, Set<String>> tagSet =
          Stream.of(tags.split(","))
              .map(String::toLowerCase)
              .map(String::trim)
              .collect(
                  Collectors.partitioningBy(
                      s -> s.startsWith("!"),
                      Collectors.mapping(
                          (String s) -> s.startsWith("!") ? s.substring(1) : s,
                          Collectors.toSet())));
      search = search.filter(map -> matchesTags(map, tagSet.get(false), tagSet.get(true)));
    }

    if (author != null) {
      search = search.filter(map -> matchesAuthor(map, author));
    }

    Set<MapInfo> maps = search.collect(Collectors.toCollection(TreeSet::new));
    int resultsPerPage = 8;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    String title =
        ComponentUtils.paginate(TextTranslations.translate("map.title", sender), page, pages);
    String listHeader =
        ComponentUtils.horizontalLineHeading(title, ChatColor.BLUE, ComponentUtils.MAX_CHAT_WIDTH);

    new PrettyPaginatedResult<MapInfo>(listHeader, resultsPerPage) {
      @Override
      public String format(MapInfo map, int index) {
        return (index + 1)
            + ". "
            + map.getStyledNameLegacy(MapNameStyle.COLOR_WITH_AUTHORS, sender);
      }
    }.display(audience, ImmutableSortedSet.copyOf(maps), page);
  }

  private static boolean matchesTags(
      MapInfo map, @Nullable Collection<String> posTags, @Nullable Collection<String> negTags) {
    int matches = 0;
    for (MapTag tag : checkNotNull(map).getTags()) {
      if (negTags != null && negTags.contains(tag.getId())) {
        return false;
      }
      if (posTags != null && posTags.contains(tag.getId())) {
        matches++;
      }
    }
    return posTags == null || matches == posTags.size();
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
            mapInfoLabel("map.info.objective"),
            new PersonalizedText(map.getDescription(), ChatColor.GOLD)));

    Collection<Contributor> authors = map.getAuthors();
    if (authors.size() == 1) {
      audience.sendMessage(
          new PersonalizedText(
              mapInfoLabel("map.info.author.singular"),
              formatContribution(authors.iterator().next())));
    } else {
      audience.sendMessage(mapInfoLabel("map.info.author.plural"));
      for (Contributor author : authors) {
        audience.sendMessage(new PersonalizedText("  ").extra(formatContribution(author)));
      }
    }

    Collection<Contributor> contributors = map.getContributors();
    if (!contributors.isEmpty()) {
      audience.sendMessage(mapInfoLabel("map.info.contributors"));
      for (Contributor contributor : contributors) {
        audience.sendMessage(new PersonalizedText("  ").extra(formatContribution(contributor)));
      }
    }

    if (!map.getRules().isEmpty()) {
      audience.sendMessage(mapInfoLabel("map.info.rules"));

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
            mapInfoLabel("map.info.playerLimit"), createPlayerLimitComponent(sender, map)));

    if (sender.hasPermission(Permissions.DEBUG)) {
      audience.sendMessage(
          new PersonalizedText(
              mapInfoLabel("map.info.proto"),
              new PersonalizedText(map.getProto().toString(), ChatColor.GOLD)));
    }

    audience.sendMessage(createTagsComponent(map.getTags()));

    if (PGM.get().getMapOrder() instanceof MapPoolManager) {
      String mapPools =
          ((MapPoolManager) PGM.get().getMapOrder())
              .getMapPools().stream()
                  .filter(pool -> pool.getMaps().contains(map))
                  .map(MapPool::getName)
                  .collect(Collectors.joining(", "));
      if (!mapPools.isEmpty()) {
        audience.sendMessage(
            new PersonalizedText(
                mapInfoLabel("map.info.pools"),
                new PersonalizedText(mapPools).color(ChatColor.GOLD).bold(false)));
      }
    }
  }

  private Component createTagsComponent(Collection<MapTag> tags) {
    checkNotNull(tags);

    Component result = mapInfoLabel("map.info.tags");
    MapTag[] mapTags = tags.toArray(new MapTag[0]);
    for (int i = 0; i < mapTags.length; i++) {
      if (i != 0) {
        result.extra(Components.space());
      }

      String mapTag = mapTags[i].getId();
      Component component =
          new PersonalizedText("#" + mapTag, ChatColor.GOLD)
              .bold(false)
              .clickEvent(ClickEvent.Action.RUN_COMMAND, "/maps -t " + mapTag)
              .hoverEvent(
                  HoverEvent.Action.SHOW_TEXT,
                  new PersonalizedTranslatable("map.info.mapTag.hover", mapTag).render());
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

    String verboseVs = " " + TextTranslations.translate("map.info.playerLimit.vs", sender) + " ";
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
      sender.sendMessage(ChatColor.RED + TextTranslations.translate("map.noNextMap", sender));
      return;
    }

    audience.sendMessage(
        TranslatableComponent.of(
            "map.nextMap",
            TextColor.DARK_PURPLE,
            next.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)));
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
