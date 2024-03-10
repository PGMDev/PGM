package tc.oc.pgm.channels;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingValue;

public class GlobalChannel implements Channel<Void> {

  @Override
  public String getDisplayName() {
    return "global";
  }

  @Override
  public String[] getAliases() {
    return new String[] {"g", "all", "shout"};
  }

  @Override
  public Character getShortcut() {
    return '!';
  }

  @Override
  public SettingValue getSetting() {
    return SettingValue.CHAT_GLOBAL;
  }

  @Override
  public boolean supportsRedirect() {
    return true;
  }

  @Override
  public Void getTarget(MatchPlayer sender, Map<String, ?> arguments) {
    return null;
  }

  @Override
  public Collection<MatchPlayer> getViewers(Void unused) {
    Set<MatchPlayer> players = new HashSet<MatchPlayer>();
    PGM.get()
        .getMatchManager()
        .getMatches()
        .forEachRemaining(match -> players.addAll(match.getPlayers()));
    return players;
  }
}
