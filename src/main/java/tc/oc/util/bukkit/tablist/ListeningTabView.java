package tc.oc.util.bukkit.tablist;

import org.bukkit.event.Listener;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.util.bukkit.identity.PlayerIdentityChangeEvent;

public interface ListeningTabView extends Listener {

  public void onViewerJoinMatch(PlayerJoinMatchEvent event);

  public void onTeamChange(PlayerPartyChangeEvent event);

  public void onNickChange(PlayerIdentityChangeEvent event);
}
