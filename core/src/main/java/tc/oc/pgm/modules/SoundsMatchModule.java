package tc.oc.pgm.modules;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.util.chat.Sound;
import tc.oc.pgm.wool.PlayerWoolPlaceEvent;

@ListenerScope(MatchScope.RUNNING)
public class SoundsMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<SoundsMatchModule> {
    @Override
    public SoundsMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new SoundsMatchModule();
    }
  }

  private static final Sound RAINDROP_SOUND = new Sound("random.levelup", 1f, 1.5f);

  private void playSound(MatchPlayer player, Sound sound) {
    if (player.getSettings().getValue(SettingKey.SOUNDS).equals(SettingValue.SOUNDS_ALL)) {
      player.playSound(sound);
    }
  }

  private void playSound(MatchPlayerState playerState, Sound sound) {
    playerState.getPlayer().ifPresent(player -> playSound(player, sound));
  }

  private void playSound(Competitor competitor, Sound sound) {
    competitor.getPlayers().forEach(player -> playSound(player, sound));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchPlayerDeath(MatchPlayerDeathEvent event) {
    ParticipantState killer = event.getKiller();
    MatchPlayer victim = event.getVictim();
    if (killer != null && !killer.getParty().equals(victim.getParty())) {
      playSound(killer, RAINDROP_SOUND);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchFinish(MatchFinishEvent event) {
    event.getWinners().forEach(player -> playSound(player, RAINDROP_SOUND));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onGoalTouch(GoalTouchEvent event) {
    ParticipantState player = event.getPlayer();
    if (player != null) {
      playSound(player, RAINDROP_SOUND);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerWoolPlace(PlayerWoolPlaceEvent event) {
    playSound(event.getPlayer(), RAINDROP_SOUND);
  }
}
