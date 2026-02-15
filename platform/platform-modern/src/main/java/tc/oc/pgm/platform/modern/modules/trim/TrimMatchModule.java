package tc.oc.pgm.platform.modern.modules.trim;

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.MatchPlayers;

@NullMarked
@ListenerScope(MatchScope.RUNNING)
public class TrimMatchModule implements MatchModule, Listener {
  private final Match match;
  private final TrimProperties properties;

  private final Map<TextColor, TrimMaterial> colors = new HashMap<>();

  public TrimMatchModule(Match match, TrimProperties properties) {
    this.match = match;
    this.properties = properties;
  }

  private @Nullable TrimMaterial getColor(MatchPlayer mp) {
    var cmp = mp.getCompetitor();
    if (cmp == null) return null;
    return colors.computeIfAbsent(cmp.getTextColor(), Trims::getNearestMaterial);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onArmorChange(EntityEquipmentChangedEvent event) {
    TrimMaterial mat = null;
    PlayerInventory inv = null;
    for (var entry : event.getEquipmentChanges().entrySet()) {
      if (!entry.getKey().isArmor()) continue;
      var item = entry.getValue().newItem();
      // Ignore non-armor or leather armor
      if (!(item.getItemMeta() instanceof ArmorMeta meta)
          || item.getItemMeta() instanceof LeatherArmorMeta) continue;

      if (mat == null) {
        var mp = match.getPlayer(event.getEntity());
        if (!MatchPlayers.canInteract(mp)) return;
        mat = getColor(mp);
        inv = mp.getInventory();
      }
      var pattern = properties.patterns().get(entry.getKey());
      if (pattern == null || mat == null) continue;

      var toApply = new ArmorTrim(mat, pattern);
      if (meta.hasTrim() && toApply.equals(meta.getTrim())) continue;

      meta.setTrim(toApply);
      item.setItemMeta(meta);
      inv.setItem(entry.getKey(), item);
    }
  }
}
