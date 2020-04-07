package tc.oc.pgm.tablist;

import org.bukkit.event.Listener;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;

public interface ListeningTabView extends Listener {

  void onViewerJoinMatch(PlayerJoinMatchEvent event);

  void onTeamChange(PlayerPartyChangeEvent event);
}
