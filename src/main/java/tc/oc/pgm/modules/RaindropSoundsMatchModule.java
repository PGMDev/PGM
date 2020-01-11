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
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.match.MatchModuleFactory;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.wool.PlayerWoolPlaceEvent;

@ListenerScope(MatchScope.RUNNING)
public class RaindropSoundsMatchModule extends MatchModule implements Listener {

  public static class Factory implements MatchModuleFactory<RaindropSoundsMatchModule> {
    @Override
    public RaindropSoundsMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new RaindropSoundsMatchModule(match);
    }
  }

  private static final Sound SOUND = new Sound("random.levelup", 1f, 1.5f);

  public RaindropSoundsMatchModule(Match match) {
    super(match);
  }

  private void play(MatchPlayer player) {
    Settings settings = player.getSettings();
    if (settings.getValue(SettingKey.RAINDROP_SOUNDS).equals(SettingValue.RAINDROP_SOUNDS_ON)) {
      player.playSound(SOUND);
    }
  }

  private void play(MatchPlayerState playerState) {
    playerState.getPlayer().ifPresent(this::play);
  }

  private void play(Competitor competitor) {
    competitor.getPlayers().forEach(this::play);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchPlayerDeath(MatchPlayerDeathEvent event) {
    ParticipantState killer = event.getKiller();
    if (killer != null) {
      play(killer);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchFinish(MatchFinishEvent event) {
    event.getWinners().forEach(this::play);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onGoalTouch(GoalTouchEvent event) {
    ParticipantState player = event.getPlayer();
    if (player != null) {
      play(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerWoolPlace(PlayerWoolPlaceEvent event) {
    play(event.getPlayer());
  }
}
