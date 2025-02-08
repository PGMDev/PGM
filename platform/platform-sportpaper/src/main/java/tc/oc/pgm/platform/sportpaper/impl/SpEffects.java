package tc.oc.pgm.platform.sportpaper.impl;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.bukkit.Effects;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
@SuppressWarnings("deprecation")
public class SpEffects implements Effects {
  @Override
  public void coloredDust(Player player, Location location, Color color) {
    player
        .spigot()
        .playEffect(
            location,
            Effect.COLOURED_DUST,
            0,
            0,
            rgbToParticle(color.getRed()),
            rgbToParticle(color.getGreen()),
            rgbToParticle(color.getBlue()),
            1,
            0,
            50);
  }

  @Override
  public void coloredDust(World world, Location location, Color color) {
    world
        .spigot()
        .playEffect(
            location,
            Effect.COLOURED_DUST,
            0,
            0,
            rgbToParticle(color.getRed()),
            rgbToParticle(color.getGreen()),
            rgbToParticle(color.getBlue()),
            1,
            0,
            50);
  }

  @Override
  public void criticalArrow(Player player, Location location) {
    player.spigot().playEffect(location, Effect.CRIT, 0, 0, 0, 0, 0, 1, 0, 50);
  }

  @Override
  public void beam(Player player, Location location, DyeColor dyeColor) {
    player
        .spigot()
        .playEffect(
            location.clone().add(0, 56, 0),
            Effect.TILE_DUST,
            Material.WOOL.getId(),
            dyeColor.getWoolData(),
            0.15f, // radius on each axis of the particle ball
            24f,
            0.15f,
            0f, // initial horizontal velocity
            40, // number of particles
            200); // radius in blocks to show particles
  }

  @Override
  public void beam(World world, Location location, DyeColor dyeColor) {
    world
        .spigot()
        .playEffect(
            location.clone().add(0, 56, 0),
            Effect.TILE_DUST,
            Material.WOOL.getId(),
            dyeColor.getWoolData(),
            0.15f, // radius on each axis of the particle ball
            24f,
            0.15f,
            0f, // initial horizontal velocity
            40, // number of particles
            200); // radius in blocks to show particles
  }

  @Override
  public void spawnFlame(World world, Location location) {
    world.spigot().playEffect(location, Effect.FLAME, 0, 0, 0, 0.15f, 0, 0, 40, 64);
  }

  @Override
  public void explosion(Player player, Location location) {
    player.spigot().playEffect(location, Effect.EXPLOSION_HUGE, 0, 0, 0f, 0f, 0f, 1f, 1, 256);
  }

  @Override
  public void blockBreak(Location location, BlockMaterialData material) {
    location.getWorld().playEffect(location, Effect.STEP_SOUND, material.encoded());
  }

  @Override
  public void renderRegion(
      Player player, Vector min, Vector max, ScheduledExecutorService executor) {
    boolean hasBottom = min.getY() >= -64, hasTop = max.getY() <= 320;
    if (!hasBottom) min.setY(-32);
    if (!hasTop) max.setY(288);

    var center = min.clone().add(max).multiply(0.5);
    var size = max.clone().subtract(min);

    float spreadMul = 0.20f;
    float countMul = 0.1f;
    float[][] directions = {
      {(float) size.getX() * spreadMul, 0, 0, (float) Math.max(10, size.getX() * countMul)},
      {0, (float) size.getY() * spreadMul, 0, (float) Math.max(10, size.getY() * countMul)},
      {0, 0, (float) size.getZ() * spreadMul, (float) Math.max(10, size.getZ() * countMul)}
    };
    if (!hasBottom) min.setY(Double.NaN);
    if (!hasTop) max.setY(Double.NaN);

    double[][] edges = {
      {center.getX(), min.getY(), min.getZ()},
      {center.getX(), min.getY(), max.getZ()},
      {center.getX(), max.getY(), min.getZ()},
      {center.getX(), max.getY(), max.getZ()},
      {min.getX(), center.getY(), min.getZ()},
      {min.getX(), center.getY(), max.getZ()},
      {max.getX(), center.getY(), min.getZ()},
      {max.getX(), center.getY(), max.getZ()},
      {min.getX(), min.getY(), center.getZ()},
      {min.getX(), max.getY(), center.getZ()},
      {max.getX(), min.getY(), center.getZ()},
      {max.getX(), max.getY(), center.getZ()},
    };
    double[][] vertices = {
      {min.getX(), min.getY(), min.getZ()},
      {min.getX(), min.getY(), max.getZ()},
      {min.getX(), max.getY(), min.getZ()},
      {min.getX(), max.getY(), max.getZ()},
      {max.getX(), min.getY(), min.getZ()},
      {max.getX(), min.getY(), max.getZ()},
      {max.getX(), max.getY(), min.getZ()},
      {max.getX(), max.getY(), max.getZ()},
    };

    class ParticleRunner implements Runnable {
      private final Future<?> task =
          executor.scheduleWithFixedDelay(this, 0, 100, TimeUnit.MILLISECONDS);
      private final Location loc = new Location(player.getWorld(), 0, 0, 0);
      private int ticks = 150;

      @Override
      public void run() {
        if (ticks-- < 0) {
          task.cancel(true);
          return;
        }
        for (int i = 0; i < edges.length; i++) {
          if (Double.isNaN(edges[i][1])) continue;
          loc.set(edges[i][0], edges[i][1], edges[i][2]);
          var dirs = directions[(i / 4)];
          player
              .spigot()
              .playEffect(
                  loc, Effect.FLAME, 0, 0, dirs[0], dirs[1], dirs[2], 0, (int) dirs[3], 256);
        }
        for (double[] vertex : vertices) {
          if (Double.isNaN(vertex[1])) continue;
          loc.set(vertex[0], vertex[1], vertex[2]);
          player.spigot().playEffect(loc, Effect.COLOURED_DUST, 0, 0, 1f, 0.01f, 0.01f, 1, 0, 256);
        }
      }
    }
    ;
    new ParticleRunner();
  }

  private float rgbToParticle(int rgb) {
    return Math.max(0.001f, (rgb / 255.0f));
  }
}
