package tc.oc.pgm.modules;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.match.MatchModuleFactory;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.wool.PlayerWoolPlaceEvent;

@ListenerScope(MatchScope.RUNNING)
public class SoundsMatchModule extends MatchModule implements Listener {

  public static class Factory implements MatchModuleFactory<SoundsMatchModule> {
    @Override
    public SoundsMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new SoundsMatchModule(match);
    }
  }

  private static final Sound RAINDROP_SOUND = new Sound("random.levelup", 1f, 1.5f);

  public SoundsMatchModule(Match match) {
    super(match);
  }

  private void playRaindrop(MatchPlayer player) {
    if (player.getSettings().getValue(SettingKey.SOUNDS).equals(SettingValue.SOUNDS_ON)) {
      player.playSound(RAINDROP_SOUND);
    }
  }

  private void playRaindrop(MatchPlayerState playerState) {
    playerState.getPlayer().ifPresent(this::playRaindrop);
  }

  private void playRaindrop(Competitor competitor) {
    competitor.getPlayers().forEach(this::playRaindrop);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchPlayerDeath(MatchPlayerDeathEvent event) {
    ParticipantState killer = event.getKiller();
    MatchPlayer victim = event.getVictim();
    if (killer != null && !killer.getId().equals(victim.getId())) {
      playRaindrop(killer);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchFinish(MatchFinishEvent event) {
    event.getWinners().forEach(this::playRaindrop);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onGoalTouch(GoalTouchEvent event) {
    ParticipantState player = event.getPlayer();
    if (player != null) {
      playRaindrop(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerWoolPlace(PlayerWoolPlaceEvent event) {
    playRaindrop(event.getPlayer());
  }
}
