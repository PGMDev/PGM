package tc.oc.pgm.platform.modern.impl;

import static tc.oc.pgm.util.material.ColorUtils.COLOR_UTILS;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import tc.oc.pgm.platform.modern.material.ModernBlockMaterialData;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.Effects;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernEffects implements Effects {
  @Override
  public void coloredDust(Player player, Location location, Color color) {
    player.spawnParticle(
        Particle.DUST, location, 0, 0d, 0d, 0d, 0d, new Particle.DustOptions(color, 1), true);
  }

  @Override
  public void coloredDust(World world, Location location, Color color) {
    world.spawnParticle(
        Particle.DUST, location, 0, 0d, 0d, 0d, 0d, new Particle.DustOptions(color, 1), true);
  }

  @Override
  public void criticalArrow(Player player, Location location) {
    player.spawnParticle(Particle.CRIT, location, 0);
  }

  @Override
  public void beam(Player player, Location location, DyeColor dyeColor) {
    player.spawnParticle(
        Particle.BLOCK,
        location.clone().add(0, 56, 0),
        40, // number of particles
        0.15f, // radius on each axis of the particle ball
        24f,
        0.15f,
        0f, // initial horizontal velocity
        COLOR_UTILS.setColor(Material.WHITE_WOOL, dyeColor).createBlockData(),
        true);
  }

  @Override
  public void beam(World world, Location location, DyeColor dyeColor) {
    world.spawnParticle(
        Particle.BLOCK,
        location.clone().add(0, 56, 0),
        40, // number of particles
        0.15f, // radius on each axis of the particle ball
        24f,
        0.15f,
        0f, // initial horizontal velocity
        COLOR_UTILS.setColor(Material.WHITE_WOOL, dyeColor).createBlockData(),
        true);
  }

  @Override
  public void spawnFlame(World world, Location location) {
    world.spawnParticle(Particle.FLAME, location, 40, 0, 0.15f, 0, 0, null, true);
  }

  @Override
  public void explosion(Player player, Location location) {
    player.spawnParticle(Particle.EXPLOSION, location, 1, 0d, 0d, 0d, 0, null, true);
  }

  @Override
  public void blockBreak(Location location, BlockMaterialData material) {
    location
        .getWorld()
        .playEffect(location, Effect.STEP_SOUND, ((ModernBlockMaterialData) material).getBlock());
  }

  @Override
  public void renderRegion(
      Player player, Vector min, Vector max, ScheduledExecutorService executor) {
    var center = min.toLocation(player.getWorld());
    var size = max.clone().subtract(min);
    var entity = player.getWorld().spawn(center, BlockDisplay.class, e -> {
      e.setVisibleByDefault(false);
      e.setPersistent(false);

      e.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());
      e.setShadowRadius(500);
      e.setShadowRadius(0);
      e.setTransformationMatrix(
          new Matrix4f().scale((float) size.getX(), (float) size.getY(), (float) size.getZ()));
    });

    player.showEntity(BukkitUtils.getPlugin(), entity);
    executor.schedule(entity::remove, 15, TimeUnit.SECONDS);
  }
}
