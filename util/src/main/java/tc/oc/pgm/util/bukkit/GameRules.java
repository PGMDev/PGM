package tc.oc.pgm.util.bukkit;

import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.platform.Platform;

// We only parse what game rules we are supporting in PGM
// TODO: evaluate what modern game rules make sense to support
public class GameRules {
  private static final Map<String, String> BY_NAME = new HashMap<>();

  static {
    parse("mob_drops", "doMobLoot");
    parse("block_drops", "doTileDrops");
    parse("keep_inventory", "keepInventory");
    parse("mob_griefing", "mobGriefing");
    parse("natural_health_regeneration", "naturalRegeneration");
    if (Platform.isModern()) {
      parse("locator_bar", "locatorBar");
    }
  }

  public static String ADVANCE_TIME = parse("advance_time", "doDaylightCycle");
  public static String FIRE_SPREAD = parse("fire_spread_radius_around_player", "doFireTick");
  public static String RANDOM_TICK_SPEED = parse("random_tick_speed", "randomTickSpeed");

  private static String parse(String... names) {
    String rule = BukkitUtils.parse(
        name -> Arrays.asList(MISC_UTILS.getGameRules()).contains(name) ? name : null, names);
    for (String name : names) {
      BY_NAME.put(StringUtils.simplify(name), rule);
    }
    return rule;
  }

  public static String getByName(String name) {
    return BY_NAME.get(StringUtils.simplify(name));
  }
}
