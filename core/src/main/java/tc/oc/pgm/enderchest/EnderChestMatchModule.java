package tc.oc.pgm.enderchest;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Maps;
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
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.RUNNING)
public class EnderChestMatchModule implements MatchModule, Listener {

  private final Match match;
  private final boolean enabled;
  private final int rows;

  private final Map<UUID, EnderChest> playerChests;

  public EnderChestMatchModule(Match match, boolean enabled, int rows) {
    this.match = match;
    this.enabled = enabled;
    this.rows = rows;
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
