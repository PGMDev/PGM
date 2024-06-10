package tc.oc.pgm.platform.sportpaper;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGH;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import com.google.gson.JsonObject;
import java.util.concurrent.ScheduledExecutorService;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.bukkit.MiscUtils;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = SPORTPAPER, priority = HIGH)
public class SportpaperMiscUtil implements MiscUtils {
  @Override
  public boolean yield(Event event) {
    event.yield();
    return true;
  }

  @Override
  public void setKnockbackReduction(Player player, float reduction) {
    player.setKnockbackReduction(reduction);
  }

  @Override
  public float getKnockbackReduction(Player player) {
    return player.getKnockbackReduction();
  }

  @Override
  public void removeDrankPotion(PlayerItemConsumeEvent event, ScheduledExecutorService ex) {
    event.setReplacement(new ItemStack(Material.AIR));
  }

  @Override
  public JsonObject getServerListExtra(ServerListPingEvent event, Plugin plugin) {
    return event.getOrCreateExtra(plugin);
  }

  @Override
  public EventException createEventException(Throwable cause, Event event) {
    return new EventException(cause, event);
  }

  @Override
  public void createExplosion(Entity source, float power, boolean fire, boolean destroy) {
    source.getWorld().createExplosion(source, source.getLocation(), power, fire, destroy);
  }
}
