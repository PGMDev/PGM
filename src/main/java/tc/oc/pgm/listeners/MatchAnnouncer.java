package tc.oc.pgm.listeners;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.chat.Sound;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.events.MatchBeginEvent;
import tc.oc.pgm.events.MatchEndEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.match.Competitor;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.teams.Team;

public class MatchAnnouncer implements Listener {

  private static final Sound SOUND_MATCH_START = new Sound("note.pling", 1f, 1.59f);
  private static final Sound SOUND_MATCH_WIN = new Sound("mob.wither.death", 1f, 1f);
  private static final Sound SOUND_MATCH_LOSE = new Sound("mob.wither.spawn", 1f, 1f);

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchBegin(final MatchBeginEvent event) {
    Match match = event.getMatch();
    match.sendMessage(
        new PersonalizedText(
            new PersonalizedTranslatable("broadcast.matchStart"), ChatColor.GREEN));

    Component go =
        new PersonalizedText(new PersonalizedTranslatable("broadcast.go"), ChatColor.GREEN);
    for (MatchPlayer player : match.getParticipatingPlayers()) {
      player.showTitle(go, null, 0, 5, 15);
    }

    match.playSound(SOUND_MATCH_START);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(final MatchEndEvent event) {
    Match match = event.getMatch();

    // broadcast match end message
    for (MatchPlayer viewer : match.getPlayers()) {
      Component title, subtitle = null;
      if (event.getWinner() == null) {
        title = new PersonalizedTranslatable("broadcast.gameOver.gameOverText");
      } else {
        title =
            new PersonalizedTranslatable(
                event.getWinner().isNamePlural()
                    ? "broadcast.gameOver.teamWinText.plural"
                    : "broadcast.gameOver.teamWinText",
                event.getWinner().getComponentName());

        if (event.getWinner() == viewer.getParty()) {
          // Winner
          viewer.playSound(SOUND_MATCH_WIN);
          if (viewer.getParty() instanceof Team) {
            subtitle =
                new PersonalizedText(
                    new PersonalizedTranslatable("broadcast.gameOver.teamWon"), ChatColor.GREEN);
          }
        } else if (viewer.getParty() instanceof Competitor) {
          // Loser
          viewer.playSound(SOUND_MATCH_LOSE);
          if (viewer.getParty() instanceof Team) {
            subtitle =
                new PersonalizedText(
                    new PersonalizedTranslatable("broadcast.gameOver.teamLost"), ChatColor.RED);
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
    event.getPlayer().getBukkit().hideTitle();
  }
}
