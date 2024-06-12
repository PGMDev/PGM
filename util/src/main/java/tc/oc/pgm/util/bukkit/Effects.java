package tc.oc.pgm.util.bukkit;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialEncoder;
import tc.oc.pgm.util.platform.Platform;

public interface Effects {
  Effects EFFECTS = Platform.requireInstance(Effects.class);

  void coloredDust(Player player, Location location, Color color);

  void coloredDust(World world, Location location, Color color);

  void criticalArrow(Player player, Location location);

  void beam(World world, Location location, DyeColor dyeColor);

  void beam(Player player, Location location, DyeColor dyeColor);

  void spawnFlame(World world, Location location);

  void explosion(Player player, Location location);

  default void blockBreak(Location location, MaterialData material) {
    location
        .getWorld()
        .playEffect(location, Effect.STEP_SOUND, MaterialEncoder.encodeMaterial(material));
  }
}
