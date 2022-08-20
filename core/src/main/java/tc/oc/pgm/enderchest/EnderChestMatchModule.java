package tc.oc.pgm.enderchest;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerPartyChangeEvent;

@ListenerScope(MatchScope.RUNNING)
public class EnderChestMatchModule implements MatchModule, Listener {

  private final Match match;
  private final boolean enabled;
  private final int rows;

  private final ImmutableList<Dropoff> dropoffs;

  private final Map<UUID, EnderChest> playerChests;

  public EnderChestMatchModule(Match match, boolean enabled, int rows, List<Dropoff> dropoffs) {
    this.match = match;
    this.enabled = enabled;
    this.rows = rows;
    this.dropoffs = ImmutableList.copyOf(dropoffs);
    this.playerChests = Maps.newHashMap();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public EnderChest getChest(MatchPlayer player) {
    EnderChest chest = playerChests.get(player.getId());
    if (chest == null) {
      chest = new EnderChest(rows);
      playerChests.put(player.getId(), chest);
    }
    return chest;
  }

  @EventHandler
  public void onParticipantLeave(PlayerPartyChangeEvent event) {
    if (!enabled) return;
    if (dropoffs.isEmpty()) return;
    Party oldParty = event.getOldParty();
    if (!(oldParty instanceof Competitor)) return;

    EnderChest chest = getChest(event.getPlayer());

    boolean dropped = false;
    for (Dropoff dropoff : dropoffs) {
      if (dropoff.getFilter().query(oldParty).isAllowed()) {
        chest.drop(dropoff.getRegion().getRandom(match.getRandom()).toLocation(match.getWorld()));
        dropped = true;
        break;
      }
    }

    if (!dropped) {
      chest.clear();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onChestInteract(PlayerInteractEvent event) {
    MatchPlayer player = match.getParticipant(event.getPlayer());
    if (event.getClickedBlock() == null) return;
    if (event.getClickedBlock().getType() != Material.ENDER_CHEST) return;
    if (player == null) return;
    if (!enabled) {
      player.sendWarning(translatable("match.disabled.enderChest"));
      return;
    }

    getChest(player).open(player);

    event.setCancelled(true);
  }
}
