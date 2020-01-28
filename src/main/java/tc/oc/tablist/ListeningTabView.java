package tc.oc.tablist;

import org.bukkit.event.Listener;
import tc.oc.identity.PlayerIdentityChangeEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;

public interface ListeningTabView extends Listener {

  public void onViewerJoinMatch(PlayerJoinMatchEvent event);

  public void onTeamChange(PlayerPartyChangeEvent event);

  public void onNickChange(PlayerIdentityChangeEvent event);
}
