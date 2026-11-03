package tc.oc.pgm.util.bukkit;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
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
import org.bukkit.scoreboard.Team;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Platform;

public interface MiscUtils {
  MiscUtils MISC_UTILS = Platform.get(MiscUtils.class);

  default boolean yield(Event event) {
    return false;
  }

  default EventException createEventException(Throwable cause, Event event) {
    return new EventException(cause);
  }

  void createExplosion(Entity source, float power, boolean fire, boolean destroy);

  PlayerDeathEvent createDeathEvent(
      Player player, EntityDamageEvent.DamageCause dmg, List<ItemStack> drops, String msg);

  EntityChangeBlockEvent createEntityChangeBlockEvent(
      Player player, Block block, BlockMaterialData md);

  int getDurationTicks(EntityCombustEvent event);

  ThrownPotion spawnPotion(Location loc, ItemStack item);

  double getArrowDamage(Arrow arrow);

  Key getSoundKey(String name);

  default void initScoreboardTeam(Team team, NamedTextColor color) {}

  boolean isPowerEnchanted(Projectile proj);

  boolean isDestructiveExplosion(EntityExplodeEvent ev);

  boolean isEntityDestroyed(EntityRemoveFromWorldEvent ev);

  Entity getFakePickupEntity(PlayerPickupItemEvent ev);
}
