package tc.oc.pgm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.event.PlayerVanishEvent;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.listeners.JoinLeaveAnnouncer.JoinVisibility;

public class VanishListener implements Listener {

  @EventHandler(priority = EventPriority.NORMAL)
  public void setVanishState(PlayerVanishEvent event) {
    MatchPlayer player = event.getPlayer();

    // Ensure player is an observer
    event.getMatch().setParty(player, event.getMatch().getDefaultParty());

    // Reset visibility to hide/show player
    player.resetVisibility();
  }

  @EventHandler(priority = EventPriority.LOW)
  public void announceLeave(PlayerVanishEvent event) {
    if (event.isVanished() && !event.isQuiet()) {
      JoinLeaveAnnouncer.leave(event.getPlayer(), JoinVisibility.NONSTAFF);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void announceJoin(PlayerVanishEvent event) {
    if (!event.isVanished() && !event.isQuiet()) {
      JoinLeaveAnnouncer.join(event.getPlayer(), JoinVisibility.NONSTAFF);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void checkMatchPlayer(MatchPlayerAddEvent event) {
    MatchPlayer player = event.getPlayer();
    // Player is joining to a team so broadcast join
    if (event.getInitialParty() instanceof Competitor) {
      Integration.setVanished(player, false, false);
    }
  }
}
