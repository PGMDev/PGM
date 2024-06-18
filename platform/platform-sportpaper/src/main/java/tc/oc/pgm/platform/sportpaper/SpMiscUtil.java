package tc.oc.pgm.platform.sportpaper;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGH;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import net.minecraft.server.v1_8_R3.EntityPotion;
import net.minecraft.server.v1_8_R3.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
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
import tc.oc.pgm.platform.sportpaper.material.LegacyMaterialData;
import tc.oc.pgm.util.bukkit.MiscUtils;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = SPORTPAPER, priority = HIGH)
public class SpMiscUtil implements MiscUtils {
  @Override
  public boolean yield(Event event) {
    event.yield();
    return true;
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

  @Override
  public PlayerDeathEvent createDeathEvent(
      Player player, EntityDamageEvent.DamageCause dmg, List<ItemStack> drops, String msg) {
    return new PlayerDeathEvent(player, drops, 0, msg);
  }

  @Override
  public EntityChangeBlockEvent createEntityChangeBlockEvent(
      Player player, Block block, BlockMaterialData md) {
    return new EntityChangeBlockEvent(
        player, block, md.getItemType(), ((LegacyMaterialData) md).getData());
  }

  @Override
  public ThrownPotion spawnPotion(Location loc, ItemStack item) {
    World world = ((CraftWorld) loc.getWorld()).getHandle();
    EntityPotion potion =
        new EntityPotion(world, loc.getX(), loc.getY(), loc.getZ(), CraftItemStack.asNMSCopy(item));
    world.addEntity(potion);
    return (ThrownPotion) potion.getBukkitEntity();
  }

  @Override
  public double getArrowDamage(Arrow arrow) {
    return arrow.spigot().getDamage();
  }
}
