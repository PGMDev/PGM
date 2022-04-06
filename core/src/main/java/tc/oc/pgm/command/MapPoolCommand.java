package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.named.MapNameStyle;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.cycle.CycleCountdown;
import tc.oc.pgm.rotation.MapPoll;
import tc.oc.pgm.rotation.MapPool;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.Rotation;
import tc.oc.pgm.rotation.VotingPool;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public final class MapPoolCommand {

  private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("00.00%");

  @Command(
      aliases = {"pool", "rotation", "rot"},
      desc = "List the maps in the map pool",
      usage = "[page] [-p pool] [-s scores] [-c chance of vote]")
  public static void pool(
      Audience audience,
      CommandSender sender,
      MapOrder mapOrder,
      @Default("1") int page,
      @Switch('r') String rotationName,
      @Switch('p') String poolName,
      @Switch('s') boolean scores,
      @Switch('c') boolean chance)
      throws CommandException {
    if (rotationName != null) poolName = rotationName;

    MapPoolManager mapPoolManager = getMapPoolManager(sender, mapOrder);
    MapPool mapPool =
        poolName == null
            ? mapPoolManager.getActiveMapPool()
            : mapPoolManager.getMapPoolByName(poolName);

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
        chances.put(map, currWeight = MapPoll.getWeight(votes.getMapScore(map)));
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

  @Command(
      aliases = {"pools", "rotations", "rots"},
      desc = "List all the map pools",
      flags = "d")
  public static void pools(
      Audience audience,
      CommandSender sender,
      MapOrder mapOrder,
      @Default("1") int page,
      @Switch('d') boolean dynamicOnly)
      throws CommandException {

    MapPoolManager mapPoolManager = getMapPoolManager(sender, mapOrder);

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

  @Command(
      aliases = {"setpool", "setrot"},
      desc = "Change the map pool",
      usage = "[pool name] -r (revert to dynamic) -t (time limit for map pool) -m (match # limit)",
      flags = "rtm",
      perms = Permissions.SETNEXT)
  public void setPool(
      Audience sender,
      CommandSender source,
      Match match,
      MapOrder mapOrder,
      @Nullable String poolName,
      @Switch('r') boolean reset,
      @Switch('t') Duration timeLimit,
      @Switch('m') Integer matchLimit)
      throws CommandException {
    if (!match.getCountdown().getAll(CycleCountdown.class).isEmpty()) {
      sender.sendMessage(translatable("admin.setPool.activeCycle", NamedTextColor.RED));
      return;
    }
    MapPoolManager mapPoolManager = getMapPoolManager(source, mapOrder);
    MapPool newPool =
        reset
            ? mapPoolManager.getAppropriateDynamicPool(match).orElse(null)
            : mapPoolManager.getMapPoolByName(poolName);

    if (newPool == null) {
      sender.sendWarning(translatable(reset ? "pool.noDynamic" : "pool.noPoolMatch"));
    } else {
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
  }

  @Command(
      aliases = {"skip"},
      desc = "Skip the next map",
      usage = "[positions]",
      perms = Permissions.SETNEXT)
  public static void skip(
      Audience viewer, CommandSender sender, MapOrder mapOrder, @Default("1") int positions)
      throws CommandException {

    if (positions < 0) {
      viewer.sendWarning(translatable("pool.skip.negative"));
      return;
    }

    MapPool pool = getMapPoolManager(sender, mapOrder).getActiveMapPool();
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

  @Command(aliases = "votenext", desc = "Vote for the next map", usage = "map")
  public static void voteNext(
      MatchPlayer player,
      CommandSender sender,
      MapOrder mapOrder,
      @Switch('o') boolean forceOpen,
      @Text MapInfo map)
      throws CommandException {
    MapPoll poll = getVotingPool(player, sender, mapOrder);
    if (poll != null) {
      boolean voteResult = poll.toggleVote(map, ((Player) sender).getUniqueId());
      Component voteAction =
          translatable(
              voteResult ? "vote.for" : "vote.abstain",
              voteResult ? NamedTextColor.GREEN : NamedTextColor.RED,
              map.getStyledName(MapNameStyle.COLOR));
      player.sendMessage(voteAction);
      poll.sendBook(player, forceOpen);
    }
  }

  @Command(aliases = "votebook", desc = "Spawn a vote book")
  public void voteBook(MatchPlayer player, CommandSender sender, MapOrder mapOrder)
      throws CommandException {
    MapPoll poll = getVotingPool(player, sender, mapOrder);
    if (poll != null) {
      poll.sendBook(player, false);
    }
  }

  private static MapPoll getVotingPool(MatchPlayer player, CommandSender sender, MapOrder mapOrder)
      throws CommandException {
    MapPool pool = getMapPoolManager(sender, mapOrder).getActiveMapPool();
    MapPoll poll = pool instanceof VotingPool ? ((VotingPool) pool).getCurrentPoll() : null;
    if (poll == null) {
      player.sendWarning(translatable("vote.noVote"));
    }
    return poll;
  }

  public static MapPoolManager getMapPoolManager(CommandSender sender, MapOrder mapOrder)
      throws CommandException {
    if (mapOrder instanceof MapPoolManager) return (MapPoolManager) mapOrder;

    throw new CommandException(TextTranslations.translate("pool.mapPoolsDisabled", sender));
  }
}
