package tc.oc.pgm.projectile.projectiles;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

public interface PgmProjectile {

  @Nullable
  Entity spawn(Player player, Vector velocity);
}
