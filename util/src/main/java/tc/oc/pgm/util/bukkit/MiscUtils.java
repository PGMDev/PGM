package tc.oc.pgm.util.bukkit;

import static tc.oc.pgm.util.bukkit.BukkitUtils.parse;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Platform;

public interface MiscUtils {
  MiscUtils MISC_UTILS = Platform.get(MiscUtils.class);

  EntityType SPLASH_POTION = parse(EntityType::valueOf, "SPLASH_POTION", "POTION");

  default boolean yield(Event event) {
    return false;
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

  default PlayerDeathEvent createDeathEvent(
      Player player, EntityDamageEvent.DamageCause dmg, List<ItemStack> drops, String msg) {
    throw new UnsupportedOperationException();
  }

  default EntityChangeBlockEvent createEntityChangeBlockEvent(
      Player player, Block block, BlockMaterialData md) {
    throw new UnsupportedOperationException();
  }

  default ThrownPotion spawnPotion(Location loc, ItemStack item) {
    ThrownPotion potion = (ThrownPotion) loc.getWorld().spawnEntity(loc, SPLASH_POTION);
    // Due to setting the item after, it causes not to show.
    // Prefer native impl when available.
    potion.setItem(item);
    return potion;
  }

  default double getArrowDamage(Arrow arrow) {
    return 2d;
  }
}
