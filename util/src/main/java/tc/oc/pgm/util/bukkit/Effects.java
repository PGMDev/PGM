package tc.oc.pgm.util.bukkit;

import net.kyori.adventure.sound.Sound;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Platform;

public interface Effects {
  Effects EFFECTS = Platform.get(Effects.class);

  void coloredDust(Player player, Location location, Color color);

  void coloredDust(World world, Location location, Color color);

  void criticalArrow(Player player, Location location);

  void beam(World world, Location location, DyeColor dyeColor);

  void beam(Player player, Location location, DyeColor dyeColor);

  void spawnFlame(World world, Location location);

  void explosion(Player player, Location location);

  void blockBreak(Location location, BlockMaterialData material);

  void spawnFlame(Player player, Location loc, float x, float y, float z, int amt);

  void pickupEffect(Entity entity, PickupEffect effect);

  enum PickupEffect {
    SPAWN(Sounds.PICKUP_APPEAR),
    DESPAWN(Sounds.PICKUP_DISAPPEAR),
    PICKUP(Sounds.PICKUP_DISAPPEAR);

    private final Sound sound;

    PickupEffect(Sound sound) {
      this.sound = sound;
    }

    public Sound sound() {
      return sound;
    }
  }
}
