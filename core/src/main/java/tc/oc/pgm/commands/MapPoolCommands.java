package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.rotation.MapPoll;
import tc.oc.pgm.rotation.MapPool;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.Rotation;
import tc.oc.pgm.rotation.VotingPool;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public class MapPoolCommands {
  private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("00.00%");

  @Command(
      aliases = {"pool", "rotation", "rot"},
      desc = "Shows the maps in the active map pool",
      usage = "[page] [-p pool] [-s scores] [-c chance of vote]",
      help = "Shows all the maps that are currently in the active map pool.")
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
      audience.sendWarning(TranslatableComponent.of("pool.noPoolMatch"));
      return;
    }
    List<MapInfo> maps = mapPool.getMaps();

    int resultsPerPage = 8;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    Component mapPoolComponent =
        TextFormatter.paginate(
            TextComponent.builder()
                .append(TranslatableComponent.of("pool.name"))
                .append(" (", TextColor.DARK_AQUA)
                .append(mapPool.getName(), TextColor.AQUA)
                .append(")", TextColor.DARK_AQUA)
                .build(),
            page,
            pages,
            TextColor.DARK_AQUA,
            TextColor.AQUA,
            false);

    Component title =
        TextFormatter.horizontalLineHeading(sender, mapPoolComponent, TextColor.BLUE, 250);

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
            TextComponent.builder()
                .append(index + ". ", nextPos == index ? TextColor.DARK_AQUA : TextColor.WHITE);
        if (votes != null && scores)
          entry.append(SCORE_FORMAT.format(votes.getMapScore(map)) + " ", TextColor.YELLOW);
        if (votes != null && chance)
          entry.append(SCORE_FORMAT.format(chances.get(map)) + " ", TextColor.YELLOW);
        entry.append(map.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS));
        return entry.build();
      }
    }.display(audience, maps, page);
  }

  @Command(
      aliases = {"pools", "rotations", "rots"},
      desc = "Shows all the existing map pools.",
      help = "Shows all the existing map pools and their trigger player counts if applicable.",
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
      audience.sendWarning(TranslatableComponent.of("pool.noMapPools"));
      return;
    }

    if (dynamicOnly) {
      mapPools = mapPools.stream().filter(MapPool::isDynamic).collect(Collectors.toList());
    }

    int resultsPerPage = 8;
    int pages = (mapPools.size() + resultsPerPage - 1) / resultsPerPage;

    Component paginated =
        TextFormatter.paginate(
            TranslatableComponent.of("pool.title"),
            page,
            pages,
            TextColor.DARK_AQUA,
            TextColor.AQUA,
            true);

    Component formattedTitle =
        TextFormatter.horizontalLineHeading(sender, paginated, TextColor.BLUE, 250);

    new PrettyPaginatedComponentResults<MapPool>(formattedTitle, resultsPerPage) {
      @Override
      public Component format(MapPool mapPool, int index) {
        Component arrow =
            TextComponent.of(
                "» ",
                mapPoolManager.getActiveMapPool().getName().equals(mapPool.getName())
                    ? TextColor.GREEN
                    : TextColor.WHITE);

        Component maps =
            TextComponent.builder()
                .append(" (", TextColor.DARK_AQUA)
                .append(TranslatableComponent.of("map.title", TextColor.DARK_GREEN))
                .append(": ", TextColor.DARK_GREEN)
                .append(Integer.toString(mapPool.getMaps().size()), TextColor.WHITE)
                .append(")", TextColor.DARK_AQUA)
                .build();

        Component players =
            TextComponent.builder()
                .append(" (", TextColor.DARK_AQUA)
                .append(TranslatableComponent.of("match.info.players", TextColor.AQUA))
                .append(": ", TextColor.AQUA)
                .append(Integer.toString(mapPool.getPlayers()), TextColor.WHITE)
                .append(")", TextColor.DARK_AQUA)
                .build();

        return TextComponent.builder()
            .append(arrow)
            .append(mapPool.getName(), TextColor.GOLD)
            .append(maps)
            .append(mapPool.isDynamic() ? players : TextComponent.empty())
            .build();
      }
    }.display(audience, mapPools, page);
  }

  @Command(
      aliases = {"skip"},
      desc = "Skips one or more maps from the current rotation.",
      usage = "[positions]",
      perms = Permissions.SETNEXT)
  public static void skip(
      Audience viewer, CommandSender sender, MapOrder mapOrder, @Default("1") int positions)
      throws CommandException {

    if (positions < 0) {
      viewer.sendWarning(TranslatableComponent.of("pool.skip.negative"));
      return;
    }

    MapPool pool = getMapPoolManager(sender, mapOrder).getActiveMapPool();
    if (!(pool instanceof Rotation)) {
      viewer.sendWarning(TranslatableComponent.of("pool.noRotation"));
      return;
    }

    ((Rotation) pool).advance(positions);

    Component message =
        TextComponent.builder()
            .append("[", TextColor.WHITE)
            .append(TranslatableComponent.of("pool.name", TextColor.GOLD))
            .append("] [", TextColor.WHITE)
            .append(pool.getName(), TextColor.AQUA)
            .append("]", TextColor.WHITE)
            .append(
                TranslatableComponent.of("pool.skip", TextColor.GREEN)
                    .args(TextComponent.of(positions, TextColor.AQUA)))
            .build();

    viewer.sendMessage(message);
  }

  @Command(aliases = "votenext", desc = "Vote for the next map to play", usage = "map")
  public static void voteNext(
      MatchPlayer player, CommandSender sender, MapOrder mapOrder, @Text MapInfo map)
      throws CommandException {
    MapPool pool = getMapPoolManager(sender, mapOrder).getActiveMapPool();
    MapPoll poll = pool instanceof VotingPool ? ((VotingPool) pool).getCurrentPoll() : null;
    if (poll == null) {
      player.sendWarning(TranslatableComponent.of("vote.noVote"));
      return;
    }
    boolean voteResult = poll.toggleVote(map, ((Player) sender).getUniqueId());

    Component voteAction =
        TranslatableComponent.of(
                voteResult ? "vote.for" : "vote.abstain",
                voteResult ? TextColor.GREEN : TextColor.RED)
            .args(map.getStyledName(MapNameStyle.COLOR));
    player.sendMessage(voteAction);
    poll.sendBook(player);
  }

  public static MapPoolManager getMapPoolManager(CommandSender sender, MapOrder mapOrder)
      throws CommandException {
    if (mapOrder instanceof MapPoolManager) return (MapPoolManager) mapOrder;

    throw new CommandException(TextTranslations.translate("pool.mapPoolsDisabled", sender));
  }
}
