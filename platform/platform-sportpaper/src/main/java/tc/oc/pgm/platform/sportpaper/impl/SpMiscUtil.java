package tc.oc.pgm.platform.sportpaper.impl;

import static net.kyori.adventure.key.Key.key;
import static tc.oc.pgm.util.platform.Supports.Priority.HIGH;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minecraft.server.v1_8_R3.EntityPotion;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftSound;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
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
  public int getDurationTicks(EntityCombustEvent event) {
    return event.getDuration();
  }

  @Override
  public ThrownPotion spawnPotion(Location loc, ItemStack item) {
    net.minecraft.server.v1_8_R3.World world = ((CraftWorld) loc.getWorld()).getHandle();
    EntityPotion potion =
        new EntityPotion(world, loc.getX(), loc.getY(), loc.getZ(), CraftItemStack.asNMSCopy(item));
    world.addEntity(potion);
    return (ThrownPotion) potion.getBukkitEntity();
  }

  @Override
  public double getArrowDamage(Arrow arrow) {
    return arrow.spigot().getDamage();
  }

  @Override
  @SuppressWarnings("PatternValidation")
  public Key getSoundKey(String name) {
    return key(CraftSound.getSound(Sound.valueOf(name)));
  }

  @Override
  public boolean isPowerEnchanted(Projectile proj) {
    // Arrows with damage > 2 are from power bows.
    return proj instanceof Arrow arrow && arrow.spigot().getDamage() > 2.0D;
  }

  @Override
  public boolean isDestructiveExplosion(EntityExplodeEvent ev) {
    // All explosions in 1.8 are destructive
    return true;
  }

  @Override
  public boolean isEntityDestroyed(EntityRemoveFromWorldEvent ev) {
    // When an entity is removed by chunk unloading, it's not destroyed
    return ev.getEntity().isDead();
  }

  @Override
  public Entity getFakePickupEntity(PlayerPickupItemEvent ev) {
    return ev.getItem();
  }
}
