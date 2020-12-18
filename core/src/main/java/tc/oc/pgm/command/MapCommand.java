package tc.oc.pgm.command;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.kyori.adventure.text.Component.text;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.rotation.MapPool;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public final class MapCommand {

  @Command(
      aliases = {"maps", "maplist", "ml"},
      desc = "List all maps loaded",
      usage = "[-a <author>] [-t <tag1>,<tag2>]")
  public void maps(
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

    Component title =
        TextFormatter.paginate(
            Component.translatable("map.title"),
            page,
            pages,
            NamedTextColor.DARK_AQUA,
            NamedTextColor.AQUA,
            true);
    Component listHeader = TextFormatter.horizontalLineHeading(sender, title, NamedTextColor.BLUE);

    new PrettyPaginatedComponentResults<MapInfo>(listHeader, resultsPerPage) {
      @Override
      public Component format(MapInfo map, int index) {
        return text()
            .append(text(Integer.toString(index + 1)))
            .append(text(". "))
            .append(
                map.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)
                    .hoverEvent(
                        HoverEvent.showText(
                            Component.translatable(
                                "command.maps.hover",
                                NamedTextColor.GRAY,
                                map.getStyledName(MapNameStyle.COLOR))))
                    .clickEvent(ClickEvent.runCommand("/map " + map.getName())))
            .build();
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
      if (contributor.getNameLegacy().toLowerCase().contains(query)) {
        return true;
      }
    }
    return false;
  }

  @Command(
      aliases = {"map", "mapinfo"},
      desc = "Show info about a map",
      usage = "[map name] - defaults to the current map")
  public void map(Audience audience, CommandSender sender, @Text MapInfo map) {
    audience.sendMessage(
        TextFormatter.horizontalLineHeading(
            sender,
            text(map.getName() + " ", NamedTextColor.DARK_AQUA)
                .append(text(map.getVersion().toString(), NamedTextColor.GRAY)),
            NamedTextColor.RED));

    audience.sendMessage(
        text()
            .append(mapInfoLabel("map.info.objective"))
            .append(text(map.getDescription(), NamedTextColor.GOLD))
            .build());

    Collection<Contributor> authors = map.getAuthors();
    if (authors.size() == 1) {
      audience.sendMessage(
          text()
              .append(mapInfoLabel("map.info.author.singular"))
              .append(formatContribution(authors.iterator().next()))
              .build());
    } else {
      audience.sendMessage(mapInfoLabel("map.info.author.plural"));
      for (Contributor author : authors) {
        audience.sendMessage(
            text()
                .append(Component.space())
                .append(Component.space())
                .append(formatContribution(author))
                .build());
      }
    }

    Collection<Contributor> contributors = map.getContributors();
    if (!contributors.isEmpty()) {
      audience.sendMessage(mapInfoLabel("map.info.contributors"));
      for (Contributor contributor : contributors) {
        audience.sendMessage(
            text()
                .append(Component.space())
                .append(Component.space())
                .append(formatContribution(contributor))
                .build());
      }
    }

    LocalDate created = map.getCreated();
    if (created != null) {
      DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
      String date = created.format(formatter);

      audience.sendMessage(
          text()
              .append(mapInfoLabel("map.info.created"))
              .append(text(date, NamedTextColor.GOLD))
              .build());
    }

    if (!map.getRules().isEmpty()) {
      audience.sendMessage(mapInfoLabel("map.info.rules"));

      int i = 0;
      for (String rule : map.getRules()) {
        audience.sendMessage(
            text()
                .append(text(++i + ") ", NamedTextColor.WHITE))
                .append(text(rule, NamedTextColor.GOLD))
                .build());
      }
    }

    audience.sendMessage(
        text()
            .append(mapInfoLabel("map.info.playerLimit"))
            .append(createPlayerLimitComponent(sender, map))
            .build());

    if (sender.hasPermission(Permissions.DEBUG)) {
      audience.sendMessage(
          text()
              .append(mapInfoLabel("map.info.proto"))
              .append(text(map.getProto().toString(), NamedTextColor.GOLD))
              .build());
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
            text()
                .append(mapInfoLabel("map.info.pools"))
                .append(text(mapPools))
                .colorIfAbsent(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, false)
                .build());
      }
    }
  }

  private Component createTagsComponent(Collection<MapTag> tags) {
    checkNotNull(tags);

    Builder result = text().append(mapInfoLabel("map.info.tags"));
    MapTag[] mapTags = tags.toArray(new MapTag[0]);
    for (int i = 0; i < mapTags.length; i++) {
      if (i != 0) {
        result.append(Component.space());
      }

      String mapTag = mapTags[i].getId();

      Component tag =
          text()
              .append(text("#"))
              .append(text(mapTag))
              .clickEvent(ClickEvent.runCommand("/maps -t " + mapTag))
              .hoverEvent(
                  HoverEvent.showText(
                      Component.translatable(
                          "map.info.mapTag.hover",
                          NamedTextColor.GRAY,
                          text(mapTag, NamedTextColor.GOLD))))
              .build();

      result.append(tag);
    }
    return result.color(NamedTextColor.GOLD).build();
  }

  private static Component createPlayerLimitComponent(CommandSender sender, MapInfo map) {
    checkNotNull(sender);
    checkNotNull(map);

    Collection<Integer> maxPlayers = map.getMaxPlayers();
    if (maxPlayers.isEmpty()) {
      return Component.empty();
    } else if (maxPlayers.size() == 1) {
      return text(maxPlayers.iterator().next().toString(), NamedTextColor.GOLD);
    }

    int totalPlayers = maxPlayers.stream().mapToInt(i -> i).sum();
    Component total = text(Integer.toString(totalPlayers), NamedTextColor.GOLD);

    String verboseVs = " " + TextTranslations.translate("map.info.playerLimit.vs", sender) + " ";
    Component verbose =
        text()
            .append(text("("))
            .append(
                text(
                    maxPlayers.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(verboseVs))))
            .append(text(")"))
            .color(NamedTextColor.GRAY)
            .build();

    return total.append(Component.space()).append(verbose);
  }

  private @Nullable Component formatContribution(Contributor contributor) {
    Component componentName = contributor.getName(NameStyle.FANCY);
    if (contributor.getContribution() == null) return componentName;
    return text()
        .append(componentName)
        .append(text(" - ", NamedTextColor.GRAY))
        .append(text(contributor.getContribution(), NamedTextColor.GRAY, TextDecoration.ITALIC))
        .build();
  }

  private Component mapInfoLabel(String key) {
    return text()
        .append(Component.translatable(key, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
        .append(text(": "))
        .build();
  }
}
