package tc.oc.pgm.modules;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.LOADED)
public class PlayerTimeMatchModule implements MatchModule, Listener {

  private final Match match;

  public PlayerTimeMatchModule(Match match) {
    this.match = match;
  }

  @EventHandler
  public void onPlayerAdded(MatchPlayerAddEvent event) {
    updatePlayerTime(event.getPlayer());
    // Upon being added to a match, set the player's preferred time
  }

  public static void updatePlayerTime(MatchPlayer player) {
    if (player.getSettings().getValue(SettingKey.TIME).equals(SettingValue.TIME_AUTO)) {
      player.getBukkit().resetPlayerTime();
      return;
    }
    player.getBukkit().setPlayerTime(getPreferencedTime(player), false);
  }

  private static long getPreferencedTime(MatchPlayer player) {
    switch (player.getSettings().getValue(SettingKey.TIME)) {
      case TIME_DARK:
        return 18000; // Midnight
      case TIME_LIGHT:
        return 6000; // Midday
      default:
        return player.getBukkit().getWorld().getFullTime();
    }
  }
}
