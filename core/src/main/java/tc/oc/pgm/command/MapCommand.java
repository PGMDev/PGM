package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.util.Assert.assertNotNull;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.specifier.Range;
import com.google.common.collect.ImmutableList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.Phase;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public final class MapCommand {

  @CommandMethod("maps|maplist|ml [page]")
  @CommandDescription("List all maps loaded")
  public void maps(
      Audience audience,
      CommandSender sender,
      MapLibrary library,
      @Argument(value = "page", defaultValue = "1") @Range(min = "1") int page,
      @Flag(value = "tags", aliases = "t", repeatable = true) List<String> tags,
      @Flag(value = "author", aliases = "a") String author,
      @Flag(value = "name", aliases = "n") String name,
      @Flag(value = "phase", aliases = "p") Phase phase) {
    Stream<MapInfo> search = library.getMaps(name);
    if (!tags.isEmpty()) {
      final Map<Boolean, Set<String>> tagSet =
          tags.stream()
              .flatMap(t -> Arrays.stream(t.split(",")))
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
      String query = StringUtils.normalize(author);
      search = search.filter(map -> matchesAuthor(map, query));
    }

    // FIXME: change when cloud gets support for default flag values
    final Phase finalPhase = phase == null ? Phase.PRODUCTION : phase;
    search = search.filter(map -> map.getPhase() == finalPhase);

    Set<MapInfo> maps = search.collect(Collectors.toCollection(TreeSet::new));
    int resultsPerPage = 8;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    Component title =
        TextFormatter.paginate(
            translatable("map.title"),
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
            .append(text(index + 1))
            .append(text(". "))
            .append(
                map.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)
                    .hoverEvent(
                        showText(
                            translatable(
                                "command.maps.hover",
                                NamedTextColor.GRAY,
                                map.getStyledName(MapNameStyle.COLOR))))
                    .clickEvent(runCommand("/map " + map.getName())))
            .build();
      }
    }.display(audience, ImmutableList.copyOf(maps), page);
  }

  private static boolean matchesTags(
      MapInfo map, Collection<String> posTags, Collection<String> negTags) {
    int matches = 0;
    for (MapTag tag : assertNotNull(map).getTags()) {
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
    for (Contributor contributor : map.getAuthors()) {
      if (StringUtils.normalize(contributor.getNameLegacy()).contains(query)) {
        return true;
      }
    }
    return false;
  }

  @CommandMethod("map|mapinfo [map]")
  @CommandDescription("Show info about a map")
  public void map(
      Audience audience,
      CommandSender sender,
      @Argument(value = "map", defaultValue = CURRENT) @Greedy MapInfo map) {
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
            text().append(space()).append(space()).append(formatContribution(author)).build());
      }
    }

    Collection<Contributor> contributors = map.getContributors();
    if (!contributors.isEmpty()) {
      audience.sendMessage(mapInfoLabel("map.info.contributors"));
      for (Contributor contributor : contributors) {
        audience.sendMessage(
            text().append(space()).append(space()).append(formatContribution(contributor)).build());
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

      audience.sendMessage(
          text()
              .append(mapInfoLabel("map.info.phase"))
              .append(map.getPhase().toComponent().color(NamedTextColor.GOLD))
              .build());
    }

    audience.sendMessage(createTagsComponent(map.getTags()));

    if (PGM.get().getMapOrder() instanceof MapPoolManager) {
      String mapPools =
          ((MapPoolManager) PGM.get().getMapOrder())
              .getMapPoolStream()
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
    assertNotNull(tags);

    Builder result = text().append(mapInfoLabel("map.info.tags"));
    MapTag[] mapTags = tags.toArray(new MapTag[0]);
    for (int i = 0; i < mapTags.length; i++) {
      if (i != 0) {
        result.append(space());
      }

      String mapTag = mapTags[i].getId();

      Component tag =
          text()
              .append(text("#"))
              .append(text(mapTag))
              .clickEvent(runCommand("/maps -t " + mapTag))
              .hoverEvent(
                  showText(
                      translatable(
                          "map.info.mapTag.hover",
                          NamedTextColor.GRAY,
                          text(mapTag, NamedTextColor.GOLD))))
              .build();

      result.append(tag);
    }
    return result.color(NamedTextColor.GOLD).build();
  }

  private static Component createPlayerLimitComponent(CommandSender sender, MapInfo map) {
    assertNotNull(sender);
    assertNotNull(map);

    Collection<Integer> maxPlayers = map.getMaxPlayers();
    if (maxPlayers.isEmpty()) {
      return empty();
    } else if (maxPlayers.size() == 1) {
      return text(maxPlayers.iterator().next().toString(), NamedTextColor.GOLD);
    }

    int totalPlayers = maxPlayers.stream().mapToInt(i -> i).sum();
    Component total = text(totalPlayers, NamedTextColor.GOLD);

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

    return total.append(space()).append(verbose);
  }

  private Component formatContribution(Contributor contributor) {
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
        .append(translatable(key, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
        .append(text(": "))
        .build();
  }
}
