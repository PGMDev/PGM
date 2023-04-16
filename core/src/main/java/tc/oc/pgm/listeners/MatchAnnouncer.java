package tc.oc.pgm.listeners;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.title.Title.title;
import static tc.oc.pgm.util.TimeUtils.fromTicks;

import com.google.common.collect.Iterables;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
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
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

public class MatchAnnouncer implements Listener {

  private static final Sound SOUND_MATCH_START =
      sound(key("note.pling"), Sound.Source.MASTER, 1f, 1.59f);
  private static final Sound SOUND_MATCH_WIN =
      sound(key("mob.wither.death"), Sound.Source.MASTER, 1f, 1f);
  private static final Sound SOUND_MATCH_LOSE =
      sound(key("mob.wither.spawn"), Sound.Source.MASTER, 1f, 1f);

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
    if (match.isFinished()) return;
    match.sendMessage(translatable("broadcast.matchStart", NamedTextColor.GREEN));

    Component go = translatable("broadcast.go", NamedTextColor.GREEN);
    match.showTitle(title(go, empty(), Title.Times.of(Duration.ZERO, fromTicks(5), fromTicks(15))));

    match.playSound(SOUND_MATCH_START);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(final MatchFinishEvent event) {
    final Match match = event.getMatch();

    // broadcast match finish message
    for (MatchPlayer viewer : match.getPlayers()) {
      Component title = null, subtitle = empty();
      final Collection<Competitor> winners = event.getWinners();
      final boolean singleWinner = winners.size() == 1;
      if (winners.isEmpty()) {
        title = translatable("broadcast.gameOver");
      } else {
        if (singleWinner) {
          title =
              translatable(
                  Iterables.getOnlyElement(winners).isNamePlural()
                      ? "broadcast.gameOver.teamWinners"
                      : "broadcast.gameOver.teamWinner",
                  TextFormatter.nameList(winners, NameStyle.FANCY, NamedTextColor.WHITE));
        }

        // Use stream here instead of #contains to avoid unchecked cast
        if (winners.stream().anyMatch(w -> w == viewer.getParty())) {
          // Winner
          viewer.playSound(SOUND_MATCH_WIN);
          if (singleWinner && viewer.getParty() instanceof Team) {
            subtitle = translatable("broadcast.gameOver.teamWon", NamedTextColor.GREEN);
          }
        } else if (viewer.getParty() instanceof Competitor) {
          // Loser
          viewer.playSound(SOUND_MATCH_LOSE);
          if (singleWinner && viewer.getParty() instanceof Team) {
            subtitle = translatable("broadcast.gameOver.teamLost", NamedTextColor.RED);
          }
        } else {
          // Observer
          viewer.playSound(SOUND_MATCH_WIN);
        }
      }

      if (title == null) {
        // 2 or more winners, show "Tied!" as the title
        title = translatable("broadcast.gameOver.tied", NamedTextColor.YELLOW);

        // If 2 or 3 winners we show the winners as the subtitle
        if (winners.size() <= 3) {
          subtitle = TextFormatter.nameList(winners, NameStyle.FANCY, NamedTextColor.WHITE);
        }
      }

      final Title.Times titleTimes = Title.Times.times(Duration.ZERO, fromTicks(40), fromTicks(40));
      viewer.showTitle(title(title, subtitle, titleTimes));

      viewer.sendMessage(title);

      if (viewer.getParty() instanceof Competitor || !singleWinner) viewer.sendMessage(subtitle);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void clearTitle(PlayerJoinMatchEvent event) {
    MatchPlayer player = event.getPlayer();
    Match match = event.getMatch();
    List<Component> extraLines = event.getExtraLines();

    player.clearTitle();

    // Bukkit assumes a player's locale is "en_US" before it receives a player's setting packet.
    // Thus, we delay sending this prominent message, so it is more likely its in the right locale.
    match
        .getExecutor(MatchScope.LOADED)
        .schedule(() -> sendWelcomeMessage(player, extraLines), 500, TimeUnit.MILLISECONDS);
  }

  public void sendWelcomeMessage(MatchPlayer viewer, List<Component> extraLines) {
    MapInfo mapInfo = viewer.getMatch().getMap();

    Component title = text(mapInfo.getName(), NamedTextColor.AQUA, TextDecoration.BOLD);
    viewer.sendMessage(
        TextFormatter.horizontalLineHeading(viewer.getBukkit(), title, NamedTextColor.WHITE, 200));

    String objective = " " + ChatColor.BLUE + ChatColor.ITALIC + mapInfo.getDescription();
    LegacyFormatUtils.wordWrap(objective, 200).forEach(m -> viewer.sendMessage(text(m)));

    Collection<Contributor> authors = mapInfo.getAuthors();
    if (!authors.isEmpty()) {
      viewer.sendMessage(
          space()
              .append(
                  translatable(
                      "misc.createdBy",
                      NamedTextColor.GRAY,
                      TextFormatter.nameList(authors, NameStyle.FANCY, NamedTextColor.GRAY))));
    }

    // Send extra info from other plugins
    for (Component extra : extraLines) {
      viewer.sendMessage(extra);
    }

    viewer.sendMessage(TextFormatter.horizontalLine(NamedTextColor.WHITE, 200));
  }

  private void sendCurrentlyPlaying(MatchPlayer viewer) {
    viewer.sendMessage(
        translatable(
            "misc.playing",
            NamedTextColor.DARK_PURPLE,
            viewer.getMatch().getMap().getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)));
  }
}
