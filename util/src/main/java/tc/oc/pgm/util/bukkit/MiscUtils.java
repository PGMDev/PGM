package tc.oc.pgm.util.bukkit;

import com.google.gson.JsonObject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.platform.Platform;

public interface MiscUtils {
  MiscUtils INSTANCE = Platform.requireInstance(MiscUtils.class);

  default boolean yield(Event event) {
    return false;
  }

  default void setKnockbackReduction(Player player, float reduction) {}

  default float getKnockbackReduction(Player player) {
    return 0;
  }

  default void removeDrankPotion(PlayerItemConsumeEvent event, ScheduledExecutorService ex) {
    int itemSlot = event.getPlayer().getInventory().getHeldItemSlot();
    Player player = event.getPlayer();

    // Setting the event item should work, but Spigot doesn't have proper error checking for
    // this so if you set the event item to air it throws a null pointer
    ex.schedule(
        () -> {
          if (player.getInventory().getItem(itemSlot).getType() == Material.GLASS_BOTTLE) {
            player.getInventory().setItem(itemSlot, new ItemStack(Material.AIR));
          }
        },
        0,
        TimeUnit.MILLISECONDS);
  }

  default JsonObject getServerListExtra(ServerListPingEvent event, Plugin plugin) {
    return new JsonObject();
  }

  default EventException createEventException(Throwable cause, Event event) {
    return new EventException(cause);
  }

  default void createExplosion(Entity source, float power, boolean fire, boolean destroy) {
    Location loc = source.getLocation();
    source.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), power, fire, destroy);
  }
}
