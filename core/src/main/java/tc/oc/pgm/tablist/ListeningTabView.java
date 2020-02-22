package tc.oc.pgm.tablist;

import org.bukkit.event.Listener;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.util.bukkit.identity.PlayerIdentityChangeEvent;

public interface ListeningTabView extends Listener {

  void onViewerJoinMatch(PlayerJoinMatchEvent event);

  void onTeamChange(PlayerPartyChangeEvent event);

  void onNickChange(PlayerIdentityChangeEvent event);
}
