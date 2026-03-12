package tc.oc.pgm.util.bukkit;

import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;

import java.util.Set;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.platform.Platform;

public interface Sounds {
  Sound ADMIN_CHAT = sound("ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP", 1f, 0.7f);
  Sound ALERT = sound("NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING", 1f, 2f);
  Sound CONTROL_POINT_GOOD = sound("PORTAL_TRAVEL", "BLOCK_PORTAL_TRAVEL", 0.35f, 2f);
  Sound CONTROL_POINT_BAD = sound("BLAZE_DEATH", "ENTITY_BLAZE_DEATH", 0.4f, 0.8f);
  Sound DEATH_OWN =
      sound("IRONGOLEM_DEATH", "ENTITY_IRON_GOLEM_DEATH"); // Own death is normal pitch, full volume
  // Kill is higher pitch, quieter
  Sound DEATH_KILLED = sound("IRONGOLEM_DEATH", "ENTITY_IRON_GOLEM_DEATH", 0.75f, 4f / 3f);
  Sound DEATH_ALLY =
      sound("IRONGOLEM_HIT", "ENTITY_IRON_GOLEM_HURT"); // Ally death is a shorter sound
  Sound DEATH_ENEMY =
      sound("IRONGOLEM_HIT", "ENTITY_IRON_GOLEM_HURT", 1f, 4f / 3f); // Enemy death is higher pitch
  Sound DEFUSE = sound("FIZZ", "ENTITY_GENERIC_EXTINGUISH_FIRE");
  Sound DIRECT_MESSAGE = sound("ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP", 1f, 1.2f);
  Sound DOUBLE_JUMP = sound("ZOMBIE_INFECT", "ENTITY_ZOMBIE_INFECT", 0.5f, 1.8f);
  Sound FALLBACK = sound("NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING");
  Sound FLAG_DROP = sound("FIREWORK_TWINKLE2", "ENTITY_FIREWORK_ROCKET_TWINKLE_FAR");
  Sound FLAG_DROP_OWN = sound("WITHER_HURT", "ENTITY_WITHER_HURT", 0.7f, 1f);
  Sound FLAG_PICKUP = sound("FIREWORK_BLAST2", "ENTITY_FIREWORK_ROCKET_BLAST_FAR", 1f, 0.7f);
  Sound FLAG_PICKUP_OWN = sound("WITHER_IDLE", "ENTITY_WITHER_AMBIENT", 0.7f, 1.2f);
  Sound FLAG_RETURN = FLAG_DROP;
  Sound FLAG_RETURN_OWN = sound("ZOMBIE_INFECT", "ENTITY_ZOMBIE_INFECT", 1.1f, 1.2f);
  Sound INVENTORY_CLICK = sound("CLICK", "BLOCK_DISPENSER_DISPENSE", 1f, 2f);
  Sound ITEM_PICKUP = sound("ITEM_PICKUP", "ENTITY_ITEM_PICKUP");
  Sound MATCH_COUNTDOWN = sound("NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING", 1f, 1.19f);
  Sound MATCH_START = sound("NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING", 1f, 1.59f);
  Sound MATCH_WIN = sound("WITHER_DEATH", "ENTITY_WITHER_DEATH");
  Sound MATCH_LOSE = sound("WITHER_SPAWN", "ENTITY_WITHER_SPAWN");
  Sound MATCH_KICK = sound("VILLAGER_HIT", "ENTITY_VILLAGER_HURT");
  Sound OBJECTIVE_FIREWORKS_FAR =
      sound("FIREWORK_BLAST2", "ENTITY_FIREWORK_ROCKET_BLAST_FAR", 0.75f, 1f);
  Sound OBJECTIVE_FIREWORKS_TWINKLE =
      sound("FIREWORK_TWINKLE2", "ENTITY_FIREWORK_ROCKET_TWINKLE_FAR", 0.75f, 1f);
  Sound OBJECTIVE_GOOD = sound("PORTAL_TRAVEL", "BLOCK_PORTAL_TRAVEL", 0.7f, 2f);
  Sound OBJECTIVE_BAD = sound("BLAZE_DEATH", "ENTITY_BLAZE_DEATH", 0.8f, 0.8f);
  Sound OBJECTIVE_MODE = sound("ZOMBIE_REMEDY", "ENTITY_ZOMBIE_VILLAGER_CURE", 0.15f, 1.2f);
  Sound PORTAL = sound("ENDERMAN_TELEPORT", "ENTITY_ENDERMAN_TELEPORT");
  Sound PROJECTILE_HIT = sound("SUCCESSFUL_HIT", "ENTITY_ARROW_HIT_PLAYER", 0.18f, 0.45f);
  Sound PROXIMITY_ALARM = sound("FIREWORK_BLAST2", "ENTITY_FIREWORK_ROCKET_BLAST_FAR", 1f, 0.7f);
  Sound RAINDROPS = sound("LEVEL_UP", "ENTITY_PLAYER_LEVELUP", 1f, 1.5f);
  Sound SCORE = sound("LEVEL_UP", "ENTITY_PLAYER_LEVELUP");
  Sound SHIELD_RECHARGE = sound("ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP", 1f, 2f);
  Sound SHOP_PURCHASE = sound("FIRE_IGNITE", "ITEM_FLINTANDSTEEL_USE", 1f, 1.4f);
  Sound TIMELIMIT_COUNTDOWN = sound("CLICK", "BLOCK_DISPENSER_DISPENSE", 0.25f, 2f);
  Sound TIMELIMIT_CRESCENDO = sound("PORTAL_TRIGGER", "BLOCK_PORTAL_TRIGGER", 1f, 0.78f);
  Sound TIP = sound("ENDERMAN_IDLE", "ENTITY_ENDERMAN_AMBIENT", 1f, 1.2f);
  Sound TNT_FUSE = sound("FUSE", "ENTITY_TNT_PRIMED");
  Sound WARNING = sound("NOTE_BASS", "BLOCK_NOTE_BLOCK_BASS", 1f, 0.75f);

  // these sounds on ≈1.14+ do not get quieter with distance, as per MC-146721.
  // see: https://github.com/PGMDev/PGM/pull/1635#issuecomment-4035905537
  Set<String> GLOBAL_SOUNDS = Set.of(
      "ambient.basalt_deltas.additions",
      "ambient.basalt_deltas.loop",
      "ambient.crimson_forest.additions",
      "ambient.crimson_forest.loop",
      "ambient.nether_wastes.additions",
      "ambient.nether_wastes.loop",
      "ambient.soul_sand_valley.additions",
      "ambient.soul_sand_valley.loop",
      "ambient.underwater.enter",
      "ambient.underwater.exit",
      "ambient.underwater.loop",
      "ambient.underwater.loop.additions",
      "ambient.underwater.loop.additions.rare",
      "ambient.underwater.loop.additions.ultra_rare",
      "ambient.warped_forest.additions",
      "ambient.warped_forest.loop",
      "block.bubble_column.upwards_inside",
      "block.bubble_column.whirlpool_inside",
      "block.dry_grass.ambient",
      "block.end_portal.spawn",
      "block.portal.travel",
      "block.portal.trigger",
      "entity.baby_nautilus.ambient_land",
      "entity.baby_nautilus.death_land",
      "entity.baby_nautilus.hurt_land",
      "entity.elder_guardian.curse",
      "entity.ender_dragon.death",
      "entity.happy_ghast.riding",
      "entity.minecart.inside",
      "entity.minecart.inside.underwater",
      "entity.nautilus.riding",
      "entity.wither.spawn",
      "item.elytra.flying",
      "item.goat_horn.sound.3",
      "music.creative",
      "music.credits",
      "music.dragon",
      "music.end",
      "music.game",
      "music.menu",
      "music.nether.basalt_deltas",
      "music.nether.crimson_forest",
      "music.nether.nether_wastes",
      "music.nether.soul_sand_valley",
      "music.overworld.badlands",
      "music.overworld.bamboo_jungle",
      "music.overworld.cherry_grove",
      "music.overworld.deep_dark",
      "music.overworld.desert",
      "music.overworld.dripstone_caves",
      "music.overworld.flower_forest",
      "music.overworld.forest",
      "music.overworld.frozen_peaks",
      "music.overworld.grove",
      "music.overworld.jagged_peaks",
      "music.overworld.jungle",
      "music.overworld.lush_caves",
      "music.overworld.meadow",
      "music.overworld.old_growth_taiga",
      "music.overworld.snowy_slopes",
      "music.overworld.sparse_jungle",
      "music.overworld.stony_peaks",
      "music.overworld.swamp",
      "music.under_water",
      "ui.button.click",
      "ui.loom.select_pattern",
      "ui.toast.challenge_complete",
      "ui.toast.in",
      "ui.toast.out",
      // 1.8 representations of the previous sounds, here because sometimes the sound gets played in
      // its "native" form on SP then ViaVersion converts it to a modern sound
      "gui.button.press",
      "minecart.inside",
      "mob.enderdragon.end",
      "mob.guardian.curse",
      "mob.wither.spawn",
      "music.game.creative",
      "music.game.end",
      "music.game.end.credits",
      "music.game.end.dragon",
      "portal.travel",
      "portal.trigger");

  static Sound sound(String legacyConstant, String modernConstant) {
    return sound(legacyConstant, modernConstant, 1f, 1f);
  }

  static Sound sound(String legacyConstant, String modernConstant, float volume, float pitch) {
    // Sound.sound due to a compiler bug
    return Sound.sound(
        MISC_UTILS.getSoundKey(Platform.isModern() ? modernConstant : legacyConstant),
        Sound.Source.MASTER,
        volume,
        pitch);
  }

  static void play(Player player, Sound sound) {
    Audience.get(player).playSound(sound);
  }

  static void play(Player player, Sound sound, Location location) {
    Audience.get(player).playSound(sound, location);
  }
}
