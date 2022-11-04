package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.specifier.FlagYielding;
import cloud.commandframework.annotations.specifier.Range;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.cycle.CycleCountdown;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.rotation.pools.Rotation;
import tc.oc.pgm.rotation.pools.VotingPool;
import tc.oc.pgm.rotation.vote.MapPoll;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.text.TextFormatter;

public final class MapPoolCommand {

  private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("00.00%");

  @CommandMethod("pool [page]")
  @CommandDescription("List the maps in the map pool")
  public void pool(
      Audience audience,
      CommandSender sender,
      MapOrder mapOrder,
      @Argument(value = "page", defaultValue = "1") @Range(min = "1") int page,
      @Flag(value = "pool", aliases = "p") MapPool mapPool,
      @Flag(value = "score", aliases = "s") boolean scores,
      @Flag(value = "chance", aliases = "c") boolean chance) {

    if (mapPool == null) mapPool = getMapPoolManager(mapOrder).getActiveMapPool();

    if (mapPool == null) {
      audience.sendWarning(translatable("pool.noPoolMatch"));
      return;
    }
    List<MapInfo> maps = mapPool.getMaps();

    int resultsPerPage = 8;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    Component mapPoolComponent =
        TextFormatter.paginate(
            text()
                .append(translatable("pool.name"))
                .append(text(" (", NamedTextColor.DARK_AQUA))
                .append(text(mapPool.getName(), NamedTextColor.AQUA))
                .append(text(")", NamedTextColor.DARK_AQUA))
                .build(),
            page,
            pages,
            NamedTextColor.DARK_AQUA,
            NamedTextColor.AQUA,
            false);

    Component title =
        TextFormatter.horizontalLineHeading(sender, mapPoolComponent, NamedTextColor.BLUE, 250);

    VotingPool votes =
        (scores || chance) && mapPool instanceof VotingPool ? (VotingPool) mapPool : null;
    Map<MapInfo, Double> chances = chance ? new HashMap<>() : null;
    if (chance && votes != null) {
      double maxWeight = 0, currWeight;
      for (MapInfo map : votes.getMaps()) {
        chances.put(map, currWeight = votes.mapPicker.getWeight(null, map, votes.getMapScore(map)));
        maxWeight += currWeight;
      }
      double finalMaxWeight = maxWeight;
      chances.replaceAll((map, weight) -> weight / finalMaxWeight);
    }

    int nextPos = mapPool instanceof Rotation ? ((Rotation) mapPool).getNextPosition() : -1;

    new PrettyPaginatedComponentResults<MapInfo>(title, resultsPerPage) {
      @Override
      public Component format(MapInfo map, int index) {
        index++;
        TextComponent.Builder entry =
            text()
                .append(
                    text(
                        index + ". ",
                        nextPos == index ? NamedTextColor.DARK_AQUA : NamedTextColor.WHITE));
        if (votes != null && scores)
          entry.append(
              text(SCORE_FORMAT.format(votes.getMapScore(map)) + " ", NamedTextColor.YELLOW));
        if (votes != null && chance)
          entry.append(text(SCORE_FORMAT.format(chances.get(map)) + " ", NamedTextColor.YELLOW));
        entry.append(map.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS));
        return entry.build();
      }
    }.display(audience, maps, page);
  }

  @CommandMethod("pools [page]")
  @CommandDescription("List all the map pools")
  public void pools(
      Audience audience,
      CommandSender sender,
      MapOrder mapOrder,
      @Argument(value = "page", defaultValue = "1") @Range(min = "1") int page,
      @Flag(value = "dynamic", aliases = "d") boolean dynamicOnly) {

    MapPoolManager mapPoolManager = getMapPoolManager(mapOrder);

    List<MapPool> mapPools = mapPoolManager.getMapPools();
    if (mapPools.isEmpty()) {
      audience.sendWarning(translatable("pool.noMapPools"));
      return;
    }

    if (dynamicOnly) {
      mapPools = mapPools.stream().filter(MapPool::isDynamic).collect(Collectors.toList());
    }

    int resultsPerPage = 8;
    int pages = (mapPools.size() + resultsPerPage - 1) / resultsPerPage;

    Component paginated =
        TextFormatter.paginate(
            translatable("pool.title"),
            page,
            pages,
            NamedTextColor.DARK_AQUA,
            NamedTextColor.AQUA,
            true);

    Component formattedTitle =
        TextFormatter.horizontalLineHeading(sender, paginated, NamedTextColor.BLUE, 250);

    new PrettyPaginatedComponentResults<MapPool>(formattedTitle, resultsPerPage) {
      @Override
      public Component format(MapPool mapPool, int index) {
        Component arrow =
            text(
                "» ",
                mapPoolManager.getActiveMapPool().getName().equals(mapPool.getName())
                    ? NamedTextColor.GREEN
                    : NamedTextColor.WHITE);

        Component maps =
            text()
                .append(text(" (", NamedTextColor.DARK_AQUA))
                .append(translatable("map.title", NamedTextColor.DARK_GREEN))
                .append(text(": ", NamedTextColor.DARK_GREEN))
                .append(text(mapPool.getMaps().size(), NamedTextColor.WHITE))
                .append(text(")", NamedTextColor.DARK_AQUA))
                .build();

        Component players =
            text()
                .append(text(" (", NamedTextColor.DARK_AQUA))
                .append(translatable("match.info.players", NamedTextColor.AQUA))
                .append(text(": ", NamedTextColor.AQUA))
                .append(text(mapPool.getPlayers(), NamedTextColor.WHITE))
                .append(text(")", NamedTextColor.DARK_AQUA))
                .build();

        return text()
            .append(arrow)
            .append(text(mapPool.getName(), NamedTextColor.GOLD))
            .append(maps)
            .append(mapPool.isDynamic() ? players : empty())
            .build();
      }
    }.display(audience, mapPools, page);
  }

  @CommandMethod("setpool [pool]")
  @CommandDescription("Change the map pool")
  @CommandPermission(Permissions.SETNEXT)
  public void setPool(
      Audience sender,
      CommandSender source,
      Match match,
      MapOrder mapOrder,
      @Argument("pool") MapPool newPool,
      @Flag(value = "reset", aliases = "r") boolean reset,
      @Flag(value = "timelimit", aliases = "t") Duration timeLimit,
      @Flag(value = "matches", aliases = "m") Integer matchLimit) {
    if (!match.getCountdown().getAll(CycleCountdown.class).isEmpty())
      throw exception("admin.setPool.activeCycle");

    MapPoolManager mapPoolManager = getMapPoolManager(mapOrder);
    if (reset)
      newPool =
          mapPoolManager
              .getAppropriateDynamicPool(match)
              .orElseThrow(() -> exception("pool.noDynamic"));
    else if (newPool == null) throw exception("pool.noPoolMatch");

    if (newPool.equals(mapPoolManager.getActiveMapPool())) {
      sender.sendMessage(
          translatable(
              "pool.matching",
              NamedTextColor.GRAY,
              text(newPool.getName(), NamedTextColor.LIGHT_PURPLE)));
      return;
    }

    mapPoolManager.updateActiveMapPool(
        newPool, match, true, source, timeLimit, matchLimit != null ? matchLimit : 0);
  }

  @CommandMethod("skip [positions]")
  @CommandDescription("Skip the next map")
  @CommandPermission(Permissions.SETNEXT)
  public void skip(
      Audience viewer,
      MapOrder mapOrder,
      @Argument(value = "positions", defaultValue = "1") @Range(min = "1") int positions) {

    MapPool pool = getMapPoolManager(mapOrder).getActiveMapPool();
    if (!(pool instanceof Rotation)) {
      viewer.sendWarning(translatable("pool.noRotation"));
      return;
    }

    ((Rotation) pool).advance(positions);

    Component message =
        text()
            .append(text("[", NamedTextColor.WHITE))
            .append(translatable("pool.name", NamedTextColor.GOLD))
            .append(text("] [", NamedTextColor.WHITE))
            .append(text(pool.getName(), NamedTextColor.AQUA))
            .append(text("]", NamedTextColor.WHITE))
            .append(
                translatable(
                    "pool.skip", NamedTextColor.GREEN, text(positions, NamedTextColor.AQUA)))
            .build();

    viewer.sendMessage(message);
  }

  @CommandMethod("votenext [map]")
  @CommandDescription("Vote for the next map")
  public void voteNext(
      MatchPlayer player,
      CommandSender sender,
      MapOrder mapOrder,
      @Flag(value = "open", aliases = "o") boolean forceOpen,
      @Argument("map") @FlagYielding MapInfo map) {
    MapPoll poll = getVotingPool(mapOrder);
    boolean voteResult = poll.toggleVote(map, ((Player) sender).getUniqueId());
    Component voteAction =
        translatable(
            voteResult ? "vote.for" : "vote.abstain",
            voteResult ? NamedTextColor.GREEN : NamedTextColor.RED,
            map.getStyledName(MapNameStyle.COLOR));
    player.sendMessage(voteAction);
    poll.sendBook(player, forceOpen);
  }

  @CommandMethod("votebook")
  @CommandDescription("Spawn a vote book")
  public void voteBook(MatchPlayer player, MapOrder mapOrder) {
    getVotingPool(mapOrder).sendBook(player, false);
  }

  private static MapPoll getVotingPool(MapOrder mapOrder) {
    MapPool pool = getMapPoolManager(mapOrder).getActiveMapPool();
    MapPoll poll = pool instanceof VotingPool ? ((VotingPool) pool).getCurrentPoll() : null;
    if (poll == null) throw exception("vote.noVote");
    return poll;
  }

  public static MapPoolManager getMapPoolManager(MapOrder mapOrder) {
    if (mapOrder instanceof MapPoolManager) return (MapPoolManager) mapOrder;
    throw exception("pool.mapPoolsDisabled");
  }
}
