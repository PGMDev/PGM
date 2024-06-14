package tc.oc.pgm.platform.v1_20_6;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
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
import tc.oc.pgm.platform.v1_20_6.material.ModernBlockMaterialData;
import tc.oc.pgm.util.bukkit.MiscUtils;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernMiscUtil implements MiscUtils {
  @Override
  public boolean yield(Event event) {
    return false;
  }

  @Override
  public void removeDrankPotion(PlayerItemConsumeEvent event, ScheduledExecutorService ex) {
    event.setReplacement(new ItemStack(Material.AIR));
  }

  @Override
  public JsonObject getServerListExtra(ServerListPingEvent event, Plugin plugin) {
    // TODO: PLATFORM 1.20 no support for extra fields in server ping
    return new JsonObject();
  }

  @Override
  public EventException createEventException(Throwable cause, Event event) {
    return new EventException(cause);
  }

  @Override
  public void createExplosion(Entity source, float power, boolean fire, boolean destroy) {
    source.getWorld().createExplosion(source, source.getLocation(), power, fire, destroy);
  }

  @Override
  @SuppressWarnings({"deprecation", "UnstableApiUsage"})
  public PlayerDeathEvent createDeathEvent(
      Player player, EntityDamageEvent.DamageCause dmg, List<ItemStack> drops, String msg) {
    return new PlayerDeathEvent(
        player, DamageSource.builder(DamageType.GENERIC_KILL).build(), drops, 0, msg);
  }

  @Override
  public EntityChangeBlockEvent createEntityChangeBlockEvent(
      Player player, Block block, BlockMaterialData md) {
    return new EntityChangeBlockEvent(player, block, ((ModernBlockMaterialData) md).getBlock());
  }

  @Override
  public ThrownPotion spawnPotion(Location loc, ItemStack item) {
    var world = ((CraftWorld) loc.getWorld()).getHandle();
    var potion = new net.minecraft.world.entity.projectile.ThrownPotion(
        world, loc.getX(), loc.getY(), loc.getZ());
    potion.setItem(CraftItemStack.asNMSCopy(item));
    world.addFreshEntity(potion);
    return (ThrownPotion) potion.getBukkitEntity();
  }

  @Override
  public double getArrowDamage(Arrow arrow) {
    return arrow.getDamage();
  }
}
