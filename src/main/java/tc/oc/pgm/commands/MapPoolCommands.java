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
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.rotation.MapOrder;
import tc.oc.pgm.rotation.MapPoll;
import tc.oc.pgm.rotation.MapPool;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.Rotation;
import tc.oc.pgm.rotation.VotingPool;
import tc.oc.pgm.util.PrettyPaginatedResult;
import tc.oc.util.components.ComponentUtils;

public class MapPoolCommands {
  private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("00.00%");

  @Command(
      aliases = {"rotation", "rot", "pool"},
      desc = "Shows the maps in the active map pool",
      usage = "[page] [-p pool] [-s scores] [-c chance of vote]",
      help = "Shows all the maps that are currently in the active map pool.")
  public static void rotation(
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
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.pools.noPoolMatch", sender));
      return;
    }
    List<MapInfo> maps = mapPool.getMaps();

    int resultsPerPage = 8;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    String title = AllTranslations.get().translate("command.pools.mapPool.title", sender);
    title +=
        ChatColor.DARK_AQUA + " (" + ChatColor.AQUA + mapPool.getName() + ChatColor.DARK_AQUA + ")";
    title = ComponentUtils.paginate(title, page, pages);
    title = ComponentUtils.horizontalLineHeading(title, ChatColor.BLUE, 250);

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

    new PrettyPaginatedResult<MapInfo>(title, resultsPerPage) {
      @Override
      public String format(MapInfo map, int index) {
        index++;
        String str = (nextPos == index ? ChatColor.DARK_AQUA + "" : "") + index + ". ";
        if (votes != null && scores)
          str += ChatColor.YELLOW + SCORE_FORMAT.format(votes.getMapScore(map)) + " ";
        if (votes != null && chance)
          str += ChatColor.YELLOW + SCORE_FORMAT.format(chances.get(map)) + " ";
        str += ChatColor.RESET + "" + map.getStyledName(NameStyle.FANCY).toLegacyText();
        return str;
      }
    }.display(audience, maps, page);
  }

  @Command(
      aliases = {"rotations", "rots", "pools"},
      desc = "Shows all the existing rotations.",
      help = "Shows all the existing rotations and their trigger player counts.")
  public static void rotations(
      Audience audience, CommandSender sender, MapOrder mapOrder, @Default("1") int page)
      throws CommandException {

    MapPoolManager mapPoolManager = getMapPoolManager(sender, mapOrder);

    List<MapPool> mapPools = mapPoolManager.getMapPools();
    if (mapPools.isEmpty()) {
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.pools.noMapPools", sender));
      return;
    }

    int resultsPerPage = 8;
    int pages = (mapPools.size() + resultsPerPage - 1) / resultsPerPage;

    String title = AllTranslations.get().translate("command.pools.mapPoolList.title", sender);
    title = ComponentUtils.paginate(title, page, pages);
    title = ComponentUtils.horizontalLineHeading(title, ChatColor.BLUE, 250);

    new PrettyPaginatedResult<MapPool>(title, resultsPerPage) {
      @Override
      public String format(MapPool mapPool, int index) {
        String arrow =
            mapPoolManager.getActiveMapPool().getName().equals(mapPool.getName())
                ? ChatColor.GREEN + "» "
                : "» ";
        return arrow
            + ChatColor.GOLD
            + mapPool.getName()
            + ChatColor.DARK_AQUA
            + " ("
            + ChatColor.AQUA
            + "Players: "
            + ChatColor.WHITE
            + mapPool.getPlayers()
            + ChatColor.DARK_AQUA
            + ")";
      }
    }.display(audience, mapPools, page);
  }

  @Command(
      aliases = {"skip"},
      desc = "Skips one or more maps from the current rotation.",
      usage = "[positions]",
      perms = Permissions.SETNEXT)
  public static void skip(CommandSender sender, MapOrder mapOrder, @Default("1") int positions)
      throws CommandException {

    if (positions < 0) {
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.pools.skip.noNegative", sender));
      return;
    }

    MapPool pool = getMapPoolManager(sender, mapOrder).getActiveMapPool();
    if (!(pool instanceof Rotation)) {
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.pools.noRotation", sender));
      return;
    }

    ((Rotation) pool).advance(positions);
    sender.sendMessage(
        ChatColor.WHITE
            + "["
            + ChatColor.GOLD
            + "Rotations"
            + ChatColor.WHITE
            + "] "
            + "["
            + ChatColor.AQUA
            + pool.getName()
            + ChatColor.WHITE
            + "] "
            + ChatColor.GREEN
            + AllTranslations.get()
                .translate(
                    "command.pools.skip.message",
                    sender,
                    (ChatColor.AQUA.toString() + positions + ChatColor.GREEN)));
  }

  @Command(aliases = "votenext", desc = "Vote for the next map to play", usage = "map")
  public static void voteNext(
      MatchPlayer player, CommandSender sender, MapOrder mapOrder, @Text MapInfo map)
      throws CommandException {
    MapPool pool = getMapPoolManager(sender, mapOrder).getActiveMapPool();
    MapPoll poll = pool instanceof VotingPool ? ((VotingPool) pool).getCurrentPoll() : null;
    if (poll == null) {
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.pool.vote.noVote", sender));
      return;
    }
    boolean voteResult = poll.toggleVote(map, ((Player) sender).getUniqueId());

    PersonalizedTranslatable tr =
        new PersonalizedTranslatable(
            voteResult ? "command.pool.vote.voted" : "command.pool.vote.removedVote",
            map.getName());
    sender.sendMessage(new PersonalizedText(tr, voteResult ? ChatColor.GREEN : ChatColor.RED));
    poll.sendBook(player);
  }

  private static MapPoolManager getMapPoolManager(CommandSender sender, MapOrder mapOrder)
      throws CommandException {
    if (mapOrder instanceof MapPoolManager) return (MapPoolManager) mapOrder;

    throw new CommandException(
        AllTranslations.get().translate("command.pools.mapPoolsDisabled", sender));
  }
}
