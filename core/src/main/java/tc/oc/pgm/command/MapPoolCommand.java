package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static tc.oc.pgm.util.text.TemporalComponent.duration;
import static tc.oc.pgm.util.text.TextException.exception;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotation.specifier.FlagYielding;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.injection.RawArgs;
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
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextFormatter;

public final class MapPoolCommand {

  private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("00.00%");

  @Command("pool [page]")
  @CommandDescription("List the maps in the map pool")
  public void pool(
      Audience sender,
      CommandSender source,
      MapPoolManager poolManager,
      @Argument("page") @Default("1") @Range(min = "1") int page,
      @Flag(value = "type", aliases = "t") MapPoolType type,
      @Flag(value = "pool", aliases = "p") MapPool mapPool,
      @Flag(value = "score", aliases = "s") boolean scores,
      @Flag(value = "chance", aliases = "c") boolean chance,
      @Flag(value = "order", aliases = "o") boolean order,
      @Flag(value = "all", aliases = "a") boolean all,
      @Flag(value = "name", aliases = "n") String name) {
    // Default to current pool
    if (mapPool == null) mapPool = poolManager.getActiveMapPool();

    if (mapPool == null || (type != null && mapPool.getType() != type))
      throw exception("pool.noPoolMatch");
    List<MapInfo> maps = mapPool.getMaps();
    if (name != null) {
      String normalized = StringUtils.normalize(name);
      maps = maps.stream()
          .filter(mi -> LiquidMetal.match(mi.getNormalizedName(), normalized))
          .toList();
    }

    int resultsPerPage = all ? maps.size() : 8;
    int pages = all ? 1 : (maps.size() + resultsPerPage - 1) / resultsPerPage;

    Component mapPoolComponent = TextFormatter.paginate(
        text()
            .append(translatable("pool.name"))
            .append(text(" (", DARK_AQUA))
            .append(text(mapPool.getName(), AQUA))
            .append(text(")", DARK_AQUA))
            .build(),
        page,
        pages,
        DARK_AQUA,
        AQUA,
        false);

    Component title = TextFormatter.horizontalLineHeading(source, mapPoolComponent, BLUE, 250);

    VotingPool votes =
        (scores || chance) && mapPool instanceof VotingPool ? (VotingPool) mapPool : null;
    Map<MapInfo, Double> chances = chance ? new HashMap<>() : null;
    if (chance && votes != null) {
      double maxWeight = 0, currWeight;
      for (MapInfo map : votes.getMaps()) {
        chances.put(map, currWeight = votes.mapPicker.getWeight(null, map, votes.getVoteData(map)));
        maxWeight += currWeight;
      }
      double finalMaxWeight = maxWeight;
      chances.replaceAll((map, weight) -> weight / finalMaxWeight);
    }

    int nextPos = mapPool instanceof Rotation ? ((Rotation) mapPool).getNextPosition() : -1;

    if (order && votes != null) {
      maps = maps.stream()
          .sorted(Comparator.comparingDouble(chance ? chances::get : votes::getMapScore)
              .reversed())
          .collect(Collectors.toList());
    }

    new PrettyPaginatedComponentResults<MapInfo>(title, resultsPerPage) {
      @Override
      public Component format(MapInfo map, int index) {
        index++;
        TextComponent.Builder r =
            text().append(text(index + ". ", nextPos == index ? DARK_AQUA : WHITE));
        if (votes != null) {
          var cd = votes.getVoteData(map).remainingCooldown(votes.constants);
          if (cd.isPositive()) r.append(duration(cd, RED).color(YELLOW)).appendSpace();
          else {
            if (scores) r.append(text(SCORE_FORMAT.format(votes.getMapScore(map)) + " ", YELLOW));
            if (chance) r.append(text(SCORE_FORMAT.format(chances.get(map)) + " ", YELLOW));
          }
        }
        r.append(map.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS));
        return r.build();
      }
    }.display(sender, maps, page);
  }

  @Command("pools [page]")
  @CommandDescription("List all the map pools")
  public void pools(
      Audience sender,
      CommandSender source,
      MapPoolManager poolManager,
      @Argument("page") @Default("1") @Range(min = "1") int page,
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
        TextFormatter.paginate(translatable("pool.title"), page, pages, DARK_AQUA, AQUA, true);

    Component formattedTitle = TextFormatter.horizontalLineHeading(source, paginated, BLUE);

    new PrettyPaginatedComponentResults<MapPool>(formattedTitle, resultsPerPage) {
      @Override
      public Component format(MapPool mapPool, int index) {
        Component arrow =
            text("» ", poolManager.getActiveMapPool().equals(mapPool) ? GREEN : WHITE);

        Component maps = text()
            .append(text(" (", DARK_AQUA))
            .append(translatable("map.title", DARK_GREEN))
            .append(text(": ", DARK_GREEN))
            .append(text(mapPool.getMaps().size(), WHITE))
            .append(text(")", DARK_AQUA))
            .build();

        Component players = text()
            .append(text(" (", DARK_AQUA))
            .append(translatable("match.info.players", AQUA))
            .append(text(": ", AQUA))
            .append(text(mapPool.getPlayers(), WHITE))
            .append(text(")", DARK_AQUA))
            .build();

        return text()
            .append(arrow)
            .append(text(mapPool.getName(), GOLD))
            .append(maps)
            .append(mapPool.isDynamic() ? players : empty())
            .build();
      }
    }.display(sender, mapPools, page);
  }

  @Command("setpool <pool>")
  @CommandDescription("Change the map pool")
  @Permission(Permissions.SETNEXT)
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
          translatable("pool.matching", GRAY, text(newPool.getName(), LIGHT_PURPLE)));
      return;
    }

    poolManager.updateActiveMapPool(
        newPool, match, true, source, timeLimit, matchLimit != null ? matchLimit : 0);
  }

  @Command("setpool reset")
  @CommandDescription("Reset the pool back to appropriate default dynamic pool")
  @Permission(Permissions.SETNEXT)
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

  @Command("skip [positions]")
  @CommandDescription("Skip the next map")
  @Permission(Permissions.SETNEXT)
  public void skip(
      Audience sender,
      MapPoolManager poolManager,
      @Argument("positions") @Default("1") @Range(min = "1") int positions) {

    MapPool pool = poolManager.getActiveMapPool();
    if (!(pool instanceof Rotation)) throw exception("pool.noRotation");

    ((Rotation) pool).advance(positions);

    Component message = text()
        .append(text("[", WHITE))
        .append(translatable("pool.name", GOLD))
        .append(text("] [", WHITE))
        .append(text(pool.getName(), AQUA))
        .append(text("]", WHITE))
        .append(translatable("pool.skip", GREEN, text(positions, AQUA)))
        .build();

    sender.sendMessage(message);
  }

  @Command("votenext [map]")
  @CommandDescription("Vote for the next map")
  public void voteNext(
      MatchPlayer player,
      MapPoll poll,
      @Flag(value = "open", aliases = "o") boolean forceOpen,
      @Argument("map") @FlagYielding MapInfo map) {
    boolean voteResult = poll.toggleVote(map, player);
    Component voteAction = translatable(
        voteResult ? "vote.for" : "vote.abstain",
        voteResult ? GREEN : RED,
        map.getStyledName(MapNameStyle.COLOR));
    player.sendMessage(voteAction);
    poll.sendBook(player, forceOpen);
  }

  @Command("votebook")
  @CommandDescription("Spawn a vote book")
  public void voteBook(MatchPlayer player, MapPoll poll) {
    poll.sendBook(player, false);
  }

  // Legacy rotation command aliases

  @Command("rot [page]")
  @CommandDescription("List the maps in the rotation. Use /pool to see unfiltered results.")
  @RawArgs
  public void rot(
      Audience sender,
      CommandSender source,
      MapPoolManager poolManager,
      @Argument("page") @Default("1") @Range(min = "1") int page,
      @Flag(value = "all", aliases = "a") boolean all,
      String[] rawArgs) {
    wrapLegacy("pool", sender, rawArgs, () -> {
      if (poolManager.getActiveMapPool().getType() != MapPoolType.ORDERED)
        throw exception("pool.noRotation");

      pool(
          sender,
          source,
          poolManager,
          page,
          MapPoolType.ORDERED,
          null,
          false,
          false,
          false,
          all,
          null);
    });
  }

  @Command("rots [page]")
  @CommandDescription("List all the rotations. Use /pools to see unfiltered results.")
  @RawArgs
  public void rots(
      Audience sender,
      CommandSender source,
      MapPoolManager poolManager,
      @Argument("page") @Default("1") @Range(min = "1") int page,
      String[] rawArgs) {
    pools(sender, source, poolManager, page, MapPoolType.ORDERED, false);
    // Always follow-up, as they're filtered results that may not error out
    sender.sendMessage(alternativeUsage(rawArgs, "pools"));
  }

  @Command("setrot <rotation>")
  @CommandDescription("Set a rotation as current pool. Use /setpool to set other types of pools.")
  @Permission(Permissions.SETNEXT)
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

  @Command("setrot reset")
  @CommandDescription(
      "Reset the rotation to default. Use /setpool to reset to other types of pools.")
  @Permission(Permissions.SETNEXT)
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

    wrapLegacy("setpool", sender, rawArgs, () -> {
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
    Component cmd = text(altCommand).color(YELLOW).decorate(TextDecoration.UNDERLINED);

    return exception(
        "command.alternativeUsage",
        cmd.hoverEvent(showText(translatable("command.clickToRun", cmd).color(GREEN)))
            .clickEvent(ClickEvent.runCommand(altCommand)));
  }
}
