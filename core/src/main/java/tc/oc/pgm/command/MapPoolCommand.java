package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.injection.RawArgs;
import cloud.commandframework.annotations.specifier.FlagYielding;
import cloud.commandframework.annotations.specifier.Range;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.cycle.CycleCountdown;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.rotation.pools.MapPoolType;
import tc.oc.pgm.rotation.pools.Rotation;
import tc.oc.pgm.rotation.pools.VotingPool;
import tc.oc.pgm.rotation.vote.MapPoll;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextFormatter;

public final class MapPoolCommand {

  private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("00.00%");

  @CommandMethod("pool [page]")
  @CommandDescription("List the maps in the map pool")
  public void pool(
      Audience sender,
      CommandSender source,
      MapPoolManager poolManager,
      @Argument(value = "page", defaultValue = "1") @Range(min = "1") int page,
      @Flag(value = "type", aliases = "t") MapPoolType type,
      @Flag(value = "pool", aliases = "p") MapPool mapPool,
      @Flag(value = "score", aliases = "s") boolean scores,
      @Flag(value = "chance", aliases = "c") boolean chance) {
    // Default to current pool
    if (mapPool == null) mapPool = poolManager.getActiveMapPool();

    if (mapPool == null || (type != null && mapPool.getType() != type))
      throw exception("pool.noPoolMatch");
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
        TextFormatter.horizontalLineHeading(source, mapPoolComponent, NamedTextColor.BLUE, 250);

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
    }.display(sender, maps, page);
  }

  @CommandMethod("pools [page]")
  @CommandDescription("List all the map pools")
  public void pools(
      Audience sender,
      CommandSender source,
      MapPoolManager poolManager,
      @Argument(value = "page", defaultValue = "1") @Range(min = "1") int page,
      @Flag(value = "type", aliases = "t") MapPoolType type,
      @Flag(value = "dynamic", aliases = "d") boolean dynamicOnly) {

    if (poolManager.getPoolSize() <= 0) throw exception("pool.noMapPools");

    Stream<MapPool> poolStream = poolManager.getMapPoolStream();
    if (dynamicOnly) poolStream = poolStream.filter(MapPool::isDynamic);
    if (type != null) poolStream = poolStream.filter(mp -> mp.getType() == type);

    List<MapPool> mapPools = poolStream.collect(Collectors.toList());
    if (mapPools.isEmpty()) throw exception("pool.noPoolMatch");

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
        TextFormatter.horizontalLineHeading(source, paginated, NamedTextColor.BLUE);

    new PrettyPaginatedComponentResults<MapPool>(formattedTitle, resultsPerPage) {
      @Override
      public Component format(MapPool mapPool, int index) {
        Component arrow =
            text(
                "» ",
                poolManager.getActiveMapPool().equals(mapPool)
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
    }.display(sender, mapPools, page);
  }

  @CommandMethod("setpool <pool>")
  @CommandDescription("Change the map pool")
  @CommandPermission(Permissions.SETNEXT)
  public void setPool(
      Audience sender,
      CommandSender source,
      Match match,
      MapPoolManager poolManager,
      @Argument("pool") MapPool newPool,
      @Flag(value = "timelimit", aliases = "t") Duration timeLimit,
      @Flag(value = "matches", aliases = "m") Integer matchLimit) {
    if (!match.getCountdown().getAll(CycleCountdown.class).isEmpty())
      throw exception("admin.setPool.activeCycle");

    if (newPool == null) throw exception("pool.noPoolMatch");

    if (newPool.equals(poolManager.getActiveMapPool())) {
      sender.sendMessage(
          translatable(
              "pool.matching",
              NamedTextColor.GRAY,
              text(newPool.getName(), NamedTextColor.LIGHT_PURPLE)));
      return;
    }

    poolManager.updateActiveMapPool(
        newPool, match, true, source, timeLimit, matchLimit != null ? matchLimit : 0);
  }

  @CommandMethod("setpool reset")
  @CommandDescription("Reset the pool back to appropriate default dynamic pool")
  @CommandPermission(Permissions.SETNEXT)
  public void resetPool(
      Audience sender,
      CommandSender source,
      Match match,
      MapPoolManager poolManager,
      @Flag(value = "timelimit", aliases = "t") Duration timeLimit,
      @Flag(value = "matches", aliases = "m") Integer matchLimit) {
    setPool(
        sender,
        source,
        match,
        poolManager,
        poolManager.getAppropriateDynamicPool(match).orElseThrow(() -> exception("pool.noDynamic")),
        timeLimit,
        matchLimit);
  }

  @CommandMethod("skip [positions]")
  @CommandDescription("Skip the next map")
  @CommandPermission(Permissions.SETNEXT)
  public void skip(
      Audience sender,
      MapPoolManager poolManager,
      @Argument(value = "positions", defaultValue = "1") @Range(min = "1") int positions) {

    MapPool pool = poolManager.getActiveMapPool();
    if (!(pool instanceof Rotation)) throw exception("pool.noRotation");

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

    sender.sendMessage(message);
  }

  @CommandMethod("votenext [map]")
  @CommandDescription("Vote for the next map")
  public void voteNext(
      MatchPlayer player,
      MapPoll poll,
      @Flag(value = "open", aliases = "o") boolean forceOpen,
      @Argument("map") @FlagYielding MapInfo map) {
    boolean voteResult = poll.toggleVote(map, player);
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
  public void voteBook(MatchPlayer player, MapPoll poll) {
    poll.sendBook(player, false);
  }

  // Legacy rotation command aliases

  @CommandMethod("rot [page]")
  @CommandDescription("List the maps in the rotation. Use /pool to see unfiltered results.")
  @RawArgs
  public void rot(
      Audience sender,
      CommandSender source,
      MapPoolManager poolManager,
      @Argument(value = "page", defaultValue = "1") @Range(min = "1") int page,
      @Flag(value = "score", aliases = "s") boolean scores,
      @Flag(value = "chance", aliases = "c") boolean chance,
      String[] rawArgs) {
    wrapLegacy(
        "pool",
        sender,
        rawArgs,
        () -> {
          if (poolManager.getActiveMapPool().getType() != MapPoolType.ORDERED)
            throw exception("pool.noRotation");

          pool(sender, source, poolManager, page, MapPoolType.ORDERED, null, scores, chance);
        });
  }

  @CommandMethod("rots [page]")
  @CommandDescription("List all the rotations. Use /pools to see unfiltered results.")
  @RawArgs
  public void rots(
      Audience sender,
      CommandSender source,
      MapPoolManager poolManager,
      @Argument(value = "page", defaultValue = "1") @Range(min = "1") int page,
      String[] rawArgs) {
    pools(sender, source, poolManager, page, MapPoolType.ORDERED, false);
    // Always follow-up, as they're filtered results that may not error out
    sender.sendMessage(alternativeUsage(rawArgs, "pools"));
  }

  @CommandMethod("setrot <rotation>")
  @CommandDescription("Set a rotation as current pool. Use /setpool to set other types of pools.")
  @CommandPermission(Permissions.SETNEXT)
  @RawArgs
  public void setRot(
      Audience sender,
      CommandSender source,
      Match match,
      MapPoolManager poolManager,
      @Argument("rotation") Rotation rotation,
      @Flag(value = "timelimit", aliases = "t") Duration timeLimit,
      @Flag(value = "matches", aliases = "m") Integer matchLimit,
      String[] rawArgs) {
    wrapLegacy(
        "setpool",
        sender,
        rawArgs,
        () -> setPool(sender, source, match, poolManager, rotation, timeLimit, matchLimit));
  }

  @CommandMethod("setrot reset")
  @CommandDescription(
      "Reset the rotation to default. Use /setpool to reset to other types of pools.")
  @CommandPermission(Permissions.SETNEXT)
  @RawArgs
  public void resetRot(
      Audience sender,
      CommandSender source,
      Match match,
      MapPoolManager poolManager,
      @Flag(value = "timelimit", aliases = "t") Duration timeLimit,
      @Flag(value = "matches", aliases = "m") Integer matchLimit,
      String[] rawArgs) {

    MapPool resetRot =
        poolManager.getAppropriateDynamicPool(match).orElseThrow(() -> exception("pool.noDynamic"));

    wrapLegacy(
        "setpool",
        sender,
        rawArgs,
        () -> {
          if (resetRot.getType() != MapPoolType.ORDERED) throw exception("pool.noRotation");
          setPool(sender, source, match, poolManager, resetRot, timeLimit, matchLimit);
        });
  }

  private void wrapLegacy(String replace, Audience sender, String[] rawArgs, Runnable task) {
    try {
      task.run();
    } catch (TextException e) {
      sender.sendWarning(e.componentMessage());
      throw alternativeUsage(rawArgs, replace);
    }
  }

  private TextException alternativeUsage(String[] rawArgs, String replace) {
    rawArgs[0] = "/" + replace;
    String altCommand = String.join(" ", rawArgs);
    Component cmd =
        text(altCommand).color(NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED);

    return exception(
        "command.alternativeUsage",
        cmd.hoverEvent(
                showText(translatable("command.clickToRun", cmd).color(NamedTextColor.GREEN)))
            .clickEvent(ClickEvent.runCommand(altCommand)));
  }
}
