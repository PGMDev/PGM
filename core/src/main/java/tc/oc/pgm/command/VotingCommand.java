package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.api.Permissions.DEV;
import static tc.oc.pgm.api.map.Phase.DEVELOPMENT;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TextException.exception;

import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.channels.ChatManager;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.VotingPool;
import tc.oc.pgm.rotation.vote.MapVotePicker;
import tc.oc.pgm.rotation.vote.VotePoolOptions;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.text.TextFormatter;

@Command("vote|votes")
public class VotingCommand {

  @Command("add [map]")
  @CommandDescription("Add a custom map to the next vote")
  @Permission(Permissions.SETNEXT)
  public void addMap(
      Audience viewer,
      CommandSender sender,
      MapOrder mapOrder,
      @Argument("map") @Greedy MapInfo map) {
    if (PGM.get().getConfiguration().enforceDevPhase()
        && DEVELOPMENT.equals(map.getPhase())
        && !sender.hasPermission(DEV)) {
      throw exception("map.setNext.notDev");
    }

    VotePoolOptions vote = getVoteOptions(mapOrder);

    Component addMessage = translatable(
        "vote.add", NamedTextColor.GRAY, player(sender), map.getStyledName(MapNameStyle.COLOR));

    if (vote.isMapAdded(map)) {
      viewer.sendWarning(addMessage);
      return;
    }

    if (vote.addMap(map)) {
      ChatManager.broadcastAdminMessage(addMessage);
    } else {
      viewer.sendWarning(translatable("vote.limit", NamedTextColor.RED));
    }
  }

  @Command("remove|rm [map]")
  @CommandDescription("Remove a custom map from the next vote")
  @Permission(Permissions.SETNEXT)
  public void removeMap(
      Audience viewer,
      CommandSender sender,
      MapOrder mapOrder,
      @Argument("map") @Greedy MapInfo map) {
    VotePoolOptions vote = getVoteOptions(mapOrder);
    if (vote.removeMap(map)) {
      ChatManager.broadcastAdminMessage(translatable(
          "vote.remove",
          NamedTextColor.GRAY,
          player(sender),
          map.getStyledName(MapNameStyle.COLOR)));
    } else {
      viewer.sendWarning(translatable("map.notFound"));
    }
  }

  @Command("mode")
  @CommandDescription("Toggle the voting mode between replace and override")
  @Permission(Permissions.SETNEXT)
  public void mode(CommandSender sender, MapOrder mapOrder) {
    VotePoolOptions vote = getVoteOptions(mapOrder);
    Component voteModeName = translatable(
        vote.toggleMode() ? "vote.mode.replace" : "vote.mode.create", NamedTextColor.LIGHT_PURPLE);
    ChatManager.broadcastAdminMessage(
        translatable("vote.toggle", NamedTextColor.GRAY, player(sender), voteModeName));
  }

  @Command("clear")
  @CommandDescription("Clear all custom map selections from the next vote")
  @Permission(Permissions.SETNEXT)
  public void clearMaps(Audience viewer, CommandSender sender, MapOrder mapOrder) {
    VotePoolOptions vote = getVoteOptions(mapOrder);

    List<Component> maps = vote.getCustomVoteMaps().stream()
        .map(mi -> mi.getStyledName(MapNameStyle.COLOR))
        .collect(Collectors.toList());
    Component clearedMsg = translatable(
        "vote.remove",
        NamedTextColor.GRAY,
        player(sender),
        TextFormatter.list(maps, NamedTextColor.GRAY));

    vote.clearMaps();

    if (maps.isEmpty()) {
      viewer.sendWarning(translatable("vote.noMapsFound"));
    } else {
      ChatManager.broadcastAdminMessage(clearedMsg);
    }
  }

  @Command("list|ls")
  @CommandDescription("View a list of maps that have been selected for the next vote")
  public void listMaps(Audience viewer, MapOrder mapOrder) {
    VotePoolOptions vote = getVoteOptions(mapOrder);

    int currentMaps = vote.getCustomVoteMaps().size();
    TextColor listNumColor = currentMaps >= MapVotePicker.MIN_CUSTOM_VOTE_OPTIONS
        ? currentMaps < MapVotePicker.MAX_VOTE_OPTIONS
            ? NamedTextColor.GREEN
            : NamedTextColor.YELLOW
        : NamedTextColor.RED;

    String modeKey = vote.isReplace() ? "replace" : "create";
    Component mode = translatable(
            String.format("vote.mode.%s", modeKey), NamedTextColor.LIGHT_PURPLE)
        .hoverEvent(showText(translatable("vote.mode.hover", NamedTextColor.AQUA)))
        .clickEvent(runCommand("/vote mode"));

    Component listMsg = text()
        .append(translatable("vote.title.selection"))
        .append(text(": ("))
        .append(text(currentMaps, listNumColor))
        .append(text("/"))
        .append(text(MapVotePicker.MAX_VOTE_OPTIONS, NamedTextColor.RED))
        .append(text(") "))
        .append(text("\u00BB", NamedTextColor.GOLD))
        .append(text(" ["))
        .append(mode)
        .append(text("]"))
        .color(NamedTextColor.GRAY)
        .build();
    viewer.sendMessage(listMsg);

    int index = 1;
    for (MapInfo mi : vote.getCustomVoteMaps()) {
      Component indexedName = text()
          .append(text(index, NamedTextColor.YELLOW))
          .append(text(". ", NamedTextColor.WHITE))
          .append(mi.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS))
          .build();
      viewer.sendMessage(indexedName);
      index++;
    }
  }

  public static VotePoolOptions getVoteOptions(MapOrder mapOrder) {
    if (!(mapOrder instanceof MapPoolManager manager)) throw exception("pool.mapPoolsDisabled");
    if (!(manager.getActiveMapPool() instanceof VotingPool pool)) throw exception("vote.disabled");
    if (pool.getCurrentPoll() != null) throw exception("vote.modify.disallow");
    return manager.getVoteOptions();
  }
}
