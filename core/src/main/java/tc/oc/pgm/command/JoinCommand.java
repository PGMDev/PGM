package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotation.specifier.FlagYielding;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.channels.ChatManager;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.join.WaitlistManager;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

public final class JoinCommand {

  @Command("join|play [team]")
  @CommandDescription("Join the match")
  @Permission(Permissions.JOIN)
  public void join(
      JoinMatchModule joiner,
      MatchPlayer player,
      @Flag(value = "force", aliases = "f") boolean force,
      @Argument("team") @FlagYielding Party team) {
    if (team != null && !(team instanceof Competitor)) {
      leave(joiner, player);
      return;
    }

    if (force && player.getBukkit().hasPermission(Permissions.JOIN_FORCE)) {
      final Party oldParty = player.getParty();
      if (joiner.forceJoin(player, (Competitor) team)) {
        ChatManager.broadcastAdminMessage(translatable(
            "join.ok.force.announce",
            player(player, NameStyle.FANCY),
            player.getName(NameStyle.FANCY),
            player.getParty().getName(),
            oldParty.getName()));
      }
    } else {
      joiner.join(player, (Competitor) team);
    }
  }

  @Command("leave|obs|spectator|spec")
  @CommandDescription("Leave the match")
  @Permission(Permissions.LEAVE)
  public void leave(JoinMatchModule joiner, MatchPlayer player) {
    joiner.leave(player, JoinRequest.empty());
  }

  @Command("waitlist [page]")
  @CommandDescription("View the waitlist to join the match")
  public void waitlist(
      JoinMatchModule joiner,
      Match match,
      CommandSender sender,
      Audience viewer,
      @Argument("page") @Default("1") @Range(min = "1") int page) {
    if (sender.hasPermission(Permissions.STAFF)) {
      sendWaitlist(joiner.getWaitlist().getEntries(), sender, viewer, page);
    } else if (!joiner.getWaitlist().sendPosition(match.getPlayer((Player) sender))) {
      viewer.sendWarning(translatable("command.waitlist.notWaitlisted"));
    }
  }

  private void sendWaitlist(
      List<WaitlistManager.Entry> waitlist, CommandSender sender, Audience viewer, int page) {
    if (waitlist.isEmpty()) {
      viewer.sendWarning(translatable("command.waitlist.empty"));
      return;
    }

    int resultsPerPage = 8;
    int pages = (waitlist.size() + resultsPerPage - 1) / resultsPerPage;
    Component title = TextFormatter.paginate(
        translatable("command.waitlist.header", text(waitlist.size(), NamedTextColor.AQUA)),
        page,
        pages,
        NamedTextColor.DARK_AQUA,
        NamedTextColor.AQUA,
        true);
    Component header = TextFormatter.horizontalLineHeading(sender, title, NamedTextColor.BLUE);

    Instant now = Instant.now();
    PrettyPaginatedComponentResults.display(
        viewer,
        waitlist,
        page,
        resultsPerPage,
        header,
        (entry, i) -> translatable(
            "command.waitlist.entry",
            NamedTextColor.GRAY,
            text(i + 1),
            player(entry.playerId(), NameStyle.VERBOSE),
            duration(Duration.between(entry.since(), now), NamedTextColor.AQUA)));
  }
}
