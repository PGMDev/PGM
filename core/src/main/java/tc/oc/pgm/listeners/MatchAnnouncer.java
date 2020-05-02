package tc.oc.pgm.listeners;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.chat.Sound;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentUtils;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

public class MatchAnnouncer implements Listener {

  private static final Sound SOUND_MATCH_START = new Sound("note.pling", 1f, 1.59f);
  private static final Sound SOUND_MATCH_WIN = new Sound("mob.wither.death", 1f, 1f);
  private static final Sound SOUND_MATCH_LOSE = new Sound("mob.wither.spawn", 1f, 1f);

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchLoad(final MatchLoadEvent event) {
    final Match match = event.getMatch();
    match
        .getExecutor(MatchScope.LOADED)
        .scheduleWithFixedDelay(
            () -> match.getPlayers().forEach(this::sendCurrentlyPlaying), 0, 5, TimeUnit.MINUTES);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchBegin(final MatchStartEvent event) {
    Match match = event.getMatch();
    match.sendMessage(new PersonalizedTranslatable("broadcast.matchStart").color(ChatColor.GREEN));

    Component go = new PersonalizedTranslatable("broadcast.go").color(ChatColor.GREEN);
    for (MatchPlayer player : match.getParticipants()) {
      player.showTitle(go, null, 0, 5, 15);
    }

    match.playSound(SOUND_MATCH_START);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(final MatchFinishEvent event) {
    Match match = event.getMatch();

    // broadcast match finish message
    for (MatchPlayer viewer : match.getPlayers()) {
      Component title, subtitle = null;
      if (event.getWinner() == null) {
        title = new PersonalizedTranslatable("broadcast.gameOver");
      } else {
        title =
            new PersonalizedTranslatable(
                event.getWinner().isNamePlural()
                    ? "broadcast.gameOver.teamWinners"
                    : "broadcast.gameOver.teamWinner",
                event.getWinner().getComponentName());

        if (event.getWinner() == viewer.getParty()) {
          // Winner
          viewer.playSound(SOUND_MATCH_WIN);
          if (viewer.getParty() instanceof Team) {
            subtitle =
                new PersonalizedTranslatable("broadcast.gameOver.teamWon").color(ChatColor.GREEN);
          }
        } else if (viewer.getParty() instanceof Competitor) {
          // Loser
          viewer.playSound(SOUND_MATCH_LOSE);
          if (viewer.getParty() instanceof Team) {
            subtitle =
                new PersonalizedTranslatable("broadcast.gameOver.teamLost").color(ChatColor.RED);
          }
        } else {
          // Observer
          viewer.playSound(SOUND_MATCH_WIN);
        }
      }

      viewer.showTitle(title, subtitle, 0, 40, 40);
      viewer.sendMessage(title);
      if (subtitle != null) viewer.sendMessage(subtitle);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void clearTitle(PlayerJoinMatchEvent event) {
    final Player player = event.getPlayer().getBukkit();

    player.hideTitle();

    // Bukkit assumes a player's locale is "en_US" before it receives a player's setting packet.
    // Thus, we delay sending this prominent message, so it is more likely its in the right locale.
    event
        .getPlayer()
        .getMatch()
        .getExecutor(MatchScope.LOADED)
        .schedule(() -> sendWelcomeMessage(event.getPlayer()), 500, TimeUnit.MILLISECONDS);
  }

  private void sendWelcomeMessage(MatchPlayer viewer) {
    MapInfo mapInfo = viewer.getMatch().getMap();

    String title = ChatColor.AQUA.toString() + ChatColor.BOLD + mapInfo.getName();
    viewer.sendMessage(ComponentUtils.horizontalLineHeading(title, ChatColor.WHITE, 200));

    String objective = " " + ChatColor.BLUE + ChatColor.ITALIC + mapInfo.getDescription();
    ComponentUtils.wordWrap(objective, 200).forEach(viewer::sendMessage);

    Collection<Contributor> authors = mapInfo.getAuthors();
    if (!authors.isEmpty()) {
      viewer.sendMessage(
          TextComponent.space()
              .append(
                  TranslatableComponent.of(
                      "misc.createdBy",
                      TextColor.GRAY,
                      TextFormatter.nameList(authors, NameStyle.FANCY, TextColor.GRAY))));
    }

    viewer.sendMessage(ComponentUtils.horizontalLine(ChatColor.WHITE, 200));
  }

  private void sendCurrentlyPlaying(MatchPlayer viewer) {
    viewer.sendMessage(
        new PersonalizedTranslatable(
                "misc.playing",
                viewer
                    .getMatch()
                    .getMap()
                    .getStyledNameLegacy(MapNameStyle.COLOR_WITH_AUTHORS, viewer.getBukkit()))
            .color(ChatColor.DARK_PURPLE));
  }
}
