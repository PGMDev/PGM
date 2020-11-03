package tc.oc.pgm.listeners;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.title.Title.title;
import static tc.oc.pgm.util.TimeUtils.fromTicks;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
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
import tc.oc.pgm.match.Observers;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

public class MatchAnnouncer implements Listener {

  private static final Sound SOUND_MATCH_START =
      Sound.sound(Key.key("note.pling"), Sound.Source.MASTER, 1f, 1.59f);
  private static final Sound SOUND_MATCH_WIN =
      Sound.sound(Key.key("mob.wither.death"), Sound.Source.MASTER, 1f, 1f);
  private static final Sound SOUND_MATCH_LOSE =
      Sound.sound(Key.key("mob.wither.spawn"), Sound.Source.MASTER, 1f, 1f);

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
    match.sendMessage(Component.translatable("broadcast.matchStart", NamedTextColor.GREEN));

    Component go = Component.translatable("broadcast.go", NamedTextColor.GREEN);
    match.showTitle(
        title(go, Component.empty(), Title.Times.of(Duration.ZERO, fromTicks(5), fromTicks(15))));

    match.playSound(SOUND_MATCH_START);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(final MatchFinishEvent event) {
    Match match = event.getMatch();

    // broadcast match finish message
    for (MatchPlayer viewer : match.getPlayers()) {
      Component title, subtitle = Component.empty();
      if (event.getWinner() == null) {
        title = Component.translatable("broadcast.gameOver");
      } else {
        title =
            Component.translatable(
                event.getWinner().isNamePlural()
                    ? "broadcast.gameOver.teamWinners"
                    : "broadcast.gameOver.teamWinner",
                event.getWinner().getName());

        if (event.getWinner() == viewer.getParty()) {
          // Winner
          viewer.playSound(SOUND_MATCH_WIN);
          if (viewer.getParty() instanceof Team) {
            subtitle = Component.translatable("broadcast.gameOver.teamWon", NamedTextColor.GREEN);
          }
        } else if (viewer.getParty() instanceof Competitor) {
          // Loser
          viewer.playSound(SOUND_MATCH_LOSE);
          if (viewer.getParty() instanceof Team) {
            subtitle = Component.translatable("broadcast.gameOver.teamLost", NamedTextColor.RED);
          }
        } else {
          // Observer
          viewer.playSound(SOUND_MATCH_WIN);
        }
      }

      viewer.showTitle(
          title(title, subtitle, Title.Times.of(Duration.ZERO, fromTicks(40), fromTicks(40))));
      viewer.sendMessage(title);
      if (!(viewer.getParty() instanceof Observers)) viewer.sendMessage(subtitle);
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
    viewer.sendMessage(
        TextFormatter.horizontalLineHeading(
            viewer.getBukkit(), text(title), NamedTextColor.WHITE, 200));

    String objective = " " + ChatColor.BLUE + ChatColor.ITALIC + mapInfo.getDescription();
    LegacyFormatUtils.wordWrap(objective, 200).forEach(m -> viewer.sendMessage(text(m)));

    Collection<Contributor> authors = mapInfo.getAuthors();
    if (!authors.isEmpty()) {
      viewer.sendMessage(
          Component.space()
              .append(
                  Component.translatable(
                      "misc.createdBy",
                      NamedTextColor.GRAY,
                      TextFormatter.nameList(authors, NameStyle.FANCY, NamedTextColor.GRAY))));
    }

    viewer.sendMessage(text(LegacyFormatUtils.horizontalLine(ChatColor.WHITE, 200)));
  }

  private void sendCurrentlyPlaying(MatchPlayer viewer) {
    viewer.sendMessage(
        Component.translatable(
            "misc.playing",
            NamedTextColor.DARK_PURPLE,
            viewer.getMatch().getMap().getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)));
  }
}
