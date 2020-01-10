package tc.oc.pgm.listeners;

import java.util.Collection;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.chat.Sound;
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
import tc.oc.pgm.util.TranslationUtils;
import tc.oc.util.components.ComponentUtils;

public class MatchAnnouncer implements Listener {

  private static final Sound SOUND_MATCH_START = new Sound("note.pling", 1f, 1.59f);
  private static final Sound SOUND_MATCH_WIN = new Sound("mob.wither.death", 1f, 1f);
  private static final Sound SOUND_MATCH_LOSE = new Sound("mob.wither.spawn", 1f, 1f);

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchLoad(final MatchLoadEvent event) {
    if (Config.Broadcast.enabled()) {
      final Match match = event.getMatch();
      match
          .getScheduler(MatchScope.LOADED)
          .runTaskTimer(
              0,
              Config.Broadcast.frequency() * 20,
              () -> match.getPlayers().forEach(this::sendCurrentlyPlaying));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchBegin(final MatchStartEvent event) {
    Match match = event.getMatch();
    match.sendMessage(
        new PersonalizedText(
            new PersonalizedTranslatable("broadcast.matchStart"), ChatColor.GREEN));

    Component go =
        new PersonalizedText(new PersonalizedTranslatable("broadcast.go"), ChatColor.GREEN);
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

    sendWelcomeMessage(event.getPlayer());
  }

  private void sendWelcomeMessage(MatchPlayer viewer) {
    MapInfo mapInfo = viewer.getMatch().getMap().getInfo();

    String title = ChatColor.AQUA.toString() + ChatColor.BOLD + mapInfo.getName();
    viewer.sendMessage(ComponentUtils.horizontalLineHeading(title, ChatColor.WHITE, 200));

    String objective = " " + ChatColor.BLUE + ChatColor.ITALIC + mapInfo.getDescription();
    ComponentUtils.wordWrap(objective, 200).forEach(viewer::sendMessage);

    Collection<Contributor> authors = mapInfo.getAuthors();
    if (!authors.isEmpty()) {
      viewer.sendMessage(
          new PersonalizedText(" ", ChatColor.DARK_GRAY)
              .extra(
                  new PersonalizedTranslatable(
                      "broadcast.welcomeMessage.createdBy",
                      TranslationUtils.nameList(NameStyle.FANCY, authors))));
    }

    viewer.sendMessage(ComponentUtils.horizontalLine(ChatColor.WHITE, 200));
  }

  private void sendCurrentlyPlaying(MatchPlayer viewer) {
    viewer.sendMessage(
        org.bukkit.ChatColor.DARK_PURPLE
            + AllTranslations.get()
                .translate(
                    "broadcast.currentlyPlaying",
                    viewer.getBukkit(),
                    viewer.getMatch().getMap().getInfo().getDescription()
                        + org.bukkit.ChatColor.DARK_PURPLE));
  }
}
