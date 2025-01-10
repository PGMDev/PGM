package tc.oc.pgm.enderchest;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;

@ListenerScope(MatchScope.LOADED)
public class EnderChestMatchModule implements MatchModule, Listener {

  private final Match match;
  private final boolean enabled;
  private final DropoffFallback fallback;

  private final ImmutableList<Dropoff> dropoffs;

  public EnderChestMatchModule(
      Match match, boolean enabled, List<Dropoff> dropoffs, DropoffFallback fallback) {
    this.match = match;
    this.enabled = enabled;
    this.dropoffs = ImmutableList.copyOf(dropoffs);
    this.fallback = fallback;
  }

  public boolean isEnabled() {
    return enabled;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinMatchEvent event) {
    event.getPlayer().getBukkit().getEnderChest().clear();
  }

  @EventHandler
  public void onParticipantLeave(PlayerPartyChangeEvent event) {
    if (!isEnabled()) return;
    if (dropoffs.isEmpty()) return;
    Party oldParty = event.getOldParty();
    if (!(oldParty instanceof Competitor)) return;

    Inventory enderchest = event.getPlayer().getBukkit().getEnderChest();

    boolean dropped = false;
    for (Dropoff dropoff : dropoffs) {
      if (dropoff.getFilter().query(oldParty).isAllowed()) {
        drop(enderchest, dropoff.getRegion().getRandom(match).toLocation(match.getWorld()));
        dropped = true;
        break;
      }
    }

    if (!dropped) {
      switch (fallback) {
        case AUTO:
          if (dropoffs.isEmpty()) {
            enderchest.clear();
          }
          break;
        case DELETE:
          enderchest.clear();
          break;
        default:
          break;
      }
    }
  }

  private void drop(Inventory inventory, Location location) {
    for (ItemStack item : inventory.getContents()) {
      if (item != null && item.getType() != Material.AIR) {
        location.getWorld().dropItem(location, item);
      }
    }
    inventory.clear();
  }
}
