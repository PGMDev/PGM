package tc.oc.pgm.platform.sportpaper.impl;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
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

  private float rgbToParticle(int rgb) {
    return Math.max(0.001f, (rgb / 255.0f));
  }
}
