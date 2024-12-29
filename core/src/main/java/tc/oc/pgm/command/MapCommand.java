package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.openUrl;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.Phase;
import tc.oc.pgm.api.map.VariantInfo;
import tc.oc.pgm.map.source.MapRoot;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public final class MapCommand {

  @Command("maps|maplist|ml [page]")
  @CommandDescription("List all maps loaded")
  public void maps(
      Audience audience,
      CommandSender sender,
      MapLibrary library,
      @Argument("page") @Default("1") @Range(min = "1") int page,
      @Flag(value = "tags", aliases = "t", repeatable = true, suggestions = "maptags")
          List<String> tags,
      @Flag(value = "author", aliases = "a") String author,
      @Flag(value = "name", aliases = "n") String name,
      @Flag(value = "phase", aliases = "p", repeatable = true) List<Phase.Phases> phases) {
    Stream<MapInfo> search = library.getMaps(name);
    if (!tags.isEmpty()) {
      final Map<Boolean, Set<String>> tagSet = tags.stream()
          .flatMap(t -> Arrays.stream(t.split(",")))
          .map(String::toLowerCase)
          .map(String::trim)
          .collect(Collectors.partitioningBy(
              s -> s.startsWith("!"),
              Collectors.mapping(
                  (String s) -> s.startsWith("!") ? s.substring(1) : s, Collectors.toSet())));
      search = search.filter(map -> matchesTags(map, tagSet.get(false), tagSet.get(true)));
    }

    Phase.Phases chosenPhases = Phase.Phases.parse(sender, phases);
    search = search.filter(map -> chosenPhases.contains(map.getPhase()));

    if (author != null) {
      List<MapInfo> collect = search.toList();
      boolean exactMatch = collect.stream().anyMatch(map -> matchesAuthor(map, author, true));
      search = collect.stream().filter(map -> matchesAuthor(map, author, exactMatch));
    }

    Set<MapInfo> maps = search.collect(Collectors.toCollection(TreeSet::new));
    int resultsPerPage = 8;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    Component title = TextFormatter.paginate(
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
            .append(map.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)
                .hoverEvent(showText(translatable(
                    "command.maps.hover",
                    NamedTextColor.GRAY,
                    map.getStyledName(MapNameStyle.COLOR))))
                .clickEvent(runCommand("/map " + map.getName())))
            .build();
      }
    }.display(audience, ImmutableList.copyOf(maps), page);
  }

  @Suggestions("maptags")
  public List<String> suggestMapTags(CommandContext<CommandSender> sender, String input) {
    int commaIdx = input.lastIndexOf(',');

    final String prefix = input.substring(0, commaIdx == -1 ? 0 : commaIdx + 1);
    final String toComplete =
        input.substring(commaIdx + 1).toLowerCase(Locale.ROOT).replace("!", "");

    return MapTag.getAllTagIds().stream()
        .filter(mt -> LiquidMetal.match(mt, toComplete))
        .flatMap(tag -> Stream.of(prefix + tag, prefix + "!" + tag))
        .collect(Collectors.toList());
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

  private static boolean matchesAuthor(MapInfo map, String query, boolean exact) {
    String lowerCaseQuery = query.toLowerCase(Locale.ROOT);
    for (Contributor contributor : map.getAuthors()) {
      String name = contributor.getNameLegacy();
      if ((exact && lowerCaseQuery.equalsIgnoreCase(name))
          || (!exact && name != null && name.toLowerCase(Locale.ROOT).contains(lowerCaseQuery))) {
        return true;
      }
    }
    return false;
  }

  @Command("map|mapinfo [map]")
  @CommandDescription("Show info about a map")
  public void map(
      Audience audience,
      CommandSender sender,
      @Argument("map") @Default(CURRENT) @Greedy MapInfo map) {
    audience.sendMessage(TextFormatter.horizontalLineHeading(
        sender,
        text(map.getName() + " ", NamedTextColor.DARK_AQUA)
            .append(text(map.getVersion().toString(), NamedTextColor.GRAY)),
        NamedTextColor.RED));

    audience.sendMessage(text()
        .append(mapInfoLabel("map.info.objective"))
        .append(text(map.getDescription(), NamedTextColor.GOLD))
        .build());

    Collection<Contributor> authors = map.getAuthors();
    if (authors.size() == 1) {
      audience.sendMessage(text()
          .append(mapInfoLabel("map.info.author.singular"))
          .append(formatContribution(authors.iterator().next()))
          .build());
    } else {
      audience.sendMessage(mapInfoLabel("map.info.author.plural"));
      for (Contributor author : authors) {
        audience.sendMessage(text()
            .append(space())
            .append(space())
            .append(formatContribution(author))
            .build());
      }
    }

    Collection<Contributor> contributors = map.getContributors();
    if (!contributors.isEmpty()) {
      audience.sendMessage(mapInfoLabel("map.info.contributors"));
      for (Contributor contributor : contributors) {
        audience.sendMessage(text()
            .append(space())
            .append(space())
            .append(formatContribution(contributor))
            .build());
      }
    }

    LocalDate created = map.getCreated();
    if (created != null) {
      DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
      String date = created.format(formatter);

      audience.sendMessage(text()
          .append(mapInfoLabel("map.info.created"))
          .append(text(date, NamedTextColor.GOLD))
          .build());
    }

    if (!map.getRules().isEmpty()) {
      audience.sendMessage(mapInfoLabel("map.info.rules"));

      int i = 0;
      for (String rule : map.getRules()) {
        audience.sendMessage(text()
            .append(text(++i + ") ", NamedTextColor.WHITE))
            .append(text(rule, NamedTextColor.GOLD))
            .build());
      }
    }

    audience.sendMessage(text()
        .append(mapInfoLabel("map.info.playerLimit"))
        .append(createPlayerLimitComponent(sender, map))
        .build());

    if (sender.hasPermission(Permissions.DEBUG)) {
      audience.sendMessage(text()
          .append(mapInfoLabel("map.info.proto"))
          .append(text(map.getProto().toString(), NamedTextColor.GOLD))
          .build());
    }

    if (sender.hasPermission(Permissions.DEBUG) || !Phase.PRODUCTION.equals(map.getPhase())) {
      audience.sendMessage(text()
          .append(mapInfoLabel("map.info.phase"))
          .append(map.getPhase().toComponent().color(NamedTextColor.GOLD))
          .build());
    }

    audience.sendMessage(createTagsComponent(map.getTags()));

    if (PGM.get().getMapOrder() instanceof MapPoolManager) {
      String mapPools = ((MapPoolManager) PGM.get().getMapOrder())
          .getMapPoolStream()
          .filter(pool -> pool.getMaps().contains(map))
          .map(MapPool::getName)
          .collect(Collectors.joining(" "));
      if (!mapPools.isEmpty()) {
        audience.sendMessage(text()
            .append(mapInfoLabel("map.info.pools"))
            .append(text(mapPools, NamedTextColor.GOLD))
            .build());
      }
    }

    if (map.getVariants().size() > 1) {
      audience.sendMessage(formatVariants(map));
    }

    if (!map.getSource().getRoot().isPrivate() || sender.hasPermission(Permissions.DEBUG)) {
      audience.sendMessage(formatMapSource(sender, map));
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

      Component tag = text()
          .append(text("#"))
          .append(text(mapTag))
          .clickEvent(runCommand("/maps -t " + mapTag))
          .hoverEvent(showText(translatable(
              "map.info.mapTag.hover", NamedTextColor.GRAY, text(mapTag, NamedTextColor.GOLD))))
          .color(NamedTextColor.GOLD)
          .build();

      result.append(tag);
    }
    return result.build();
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
    Component verbose = text()
        .append(text("("))
        .append(
            text(maxPlayers.stream().map(Object::toString).collect(Collectors.joining(verboseVs))))
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
        .append(text(": ", NamedTextColor.WHITE))
        .build();
  }

  private ComponentLike formatVariants(MapInfo map) {
    TextComponent.Builder text =
        text().append(mapInfoLabel("map.info.variants")).color(NamedTextColor.GOLD);

    for (VariantInfo variant : map.getVariants().values()) {
      TextComponent variantComp;
      if (map.getVariantId().equals(variant.getVariantId())) {
        variantComp = text(variant.getVariantId(), null, TextDecoration.UNDERLINED)
            .hoverEvent(showText(translatable("map.info.variants.current", NamedTextColor.GRAY)));
      } else if (variant.isServerSupported()) {
        variantComp = text(variant.getVariantId())
            .hoverEvent(showText(translatable(
                "command.maps.hover",
                NamedTextColor.GRAY,
                text(variant.getName(), NamedTextColor.GOLD))))
            .clickEvent(runCommand("/map " + variant.getName()));
      } else {
        variantComp = text(variant.getVariantId(), NamedTextColor.RED)
            .hoverEvent(showText(translatable(
                "command.maps.unsupported",
                NamedTextColor.GRAY,
                text(variant.getName(), NamedTextColor.GOLD))));
      }
      text.append(variantComp).append(text(" "));
    }
    return text;
  }

  @NotNull
  private ComponentLike formatMapSource(CommandSender sender, MapInfo map) {
    MapSource source = map.getSource();
    MapRoot root = source.getRoot();

    Builder xmlText = text()
        .append(mapInfoLabel("map.info.xml"))
        .append(getRepository(root))
        .append(space())
        .append(text("/" + source.getRelativeDir().toString(), NamedTextColor.GOLD)
            .hoverEvent(showText(translatable("map.info.xml.path", NamedTextColor.GOLD))));

    if (root.getBaseUrl() != null && root.getRemoteHost() != null) {
      String relativeUrl = source.getRelativeXml().toString().replace(File.separatorChar, '/');
      String mapPath = root.getBaseUrl() + URLEncoder.encode(relativeUrl, StandardCharsets.UTF_8);
      xmlText.append(text()
          .color(NamedTextColor.GREEN)
          .decorate(TextDecoration.BOLD)
          .append(text(" ["))
          .append(translatable("map.info.xml.view"))
          .append(text("]"))
          .clickEvent(openUrl(mapPath))
          .hoverEvent(showText(translatable("map.info.xml.tip", NamedTextColor.GREEN))));
    }

    if (ShowXmlCommand.isEnabledFor(sender)) {
      xmlText.append(text()
          .color(NamedTextColor.AQUA)
          .decorate(TextDecoration.BOLD)
          .append(text(" ["))
          .append(translatable("map.info.xml.edit"))
          .append(text("]"))
          .clickEvent(runCommand("/showxml " + map.getName()))
          .hoverEvent(showText(translatable("map.info.xml.edit.tip", NamedTextColor.AQUA))));
    }
    return xmlText;
  }

  private ComponentLike getRepository(MapRoot root) {
    String repoUrl =
        root.getRemoteHost() != null ? root.getRemoteHost() + "/" + root.getDisplayName() : null;

    Component hover = repoUrl == null
        ? translatable("map.info.xml.repository.local", NamedTextColor.YELLOW)
        : translatable(
            "map.info.xml.repository.remote",
            NamedTextColor.YELLOW,
            text(repoUrl, NamedTextColor.GREEN));

    return text(root.getDisplayName(), NamedTextColor.YELLOW)
        .hoverEvent(showText(hover))
        .clickEvent(root.getBaseUrl() == null ? null : openUrl(root.getBaseUrl()));
  }
}
