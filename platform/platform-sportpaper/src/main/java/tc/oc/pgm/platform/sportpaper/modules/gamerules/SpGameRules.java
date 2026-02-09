package tc.oc.pgm.platform.sportpaper.modules.gamerules;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.server.v1_8_R3.GameRules;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.util.bukkit.GameRule;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
@NullMarked
public class SpGameRules implements tc.oc.pgm.util.bukkit.GameRules.GameRuleRegistry {
  private static final @Unmodifiable Map<String, GameRule<?>> GAME_RULES = new Builder().build();

  private static Class<?> getType(GameRules rules, String ruleName) {
    if (rules.a(ruleName, GameRules.EnumGameRuleType.BOOLEAN_VALUE)) {
      return Boolean.class;
    } else if (rules.a(ruleName, GameRules.EnumGameRuleType.NUMERICAL_VALUE)) {
      return Integer.class;
    } else {
      return String.class;
    }
  }

  @Override
  public @Unmodifiable Map<String, GameRule<?>> getGameRules() {
    return GAME_RULES;
  }

  private static class Builder {
    private final GameRules nmsRules = new GameRules();
    private final Map<String, GameRule<?>> rules = new HashMap<>();

    private @Unmodifiable Map<String, GameRule<?>> build() {
      for (String gameRule : nmsRules.getGameRules()) {
        rules.put(
            gameRule,
            new SpKVGameRule<>(gameRule, getType(nmsRules, gameRule), nmsRules.get(gameRule)));
      }

      registerRemapped(
          "fire_spread_radius_around_player",
          "doFireTick",
          Integer.class,
          (Boolean v) -> v ? 128 : 0,
          v -> v != 0);
      registerModern("mob_griefing", "mobGriefing");
      registerModern("keep_inventory", "keepInventory");
      registerModern("spawn_mobs", "doMobSpawning");
      registerModern("mob_drops", "doMobLoot");
      registerModern("block_drops", "doTileDrops");
      registerModern("entity_drops", "doEntityDrops");
      registerModern("command_block_output", "commandBlockOutput");
      registerModern("natural_health_regeneration", "naturalRegeneration");
      registerModern("advance_time", "doDaylightCycle");
      registerModern("log_admin_commands", "logAdminCommands");
      registerModern("show_death_messages", "showDeathMessages");
      registerModern("random_tick_speed", "randomTickSpeed");
      registerModern("send_command_feedback", "sendCommandFeedback");
      registerModern("reduced_debug_info", "reducedDebugInfo");

      return Map.copyOf(rules);
    }

    private void registerModern(String modernName, String oldName) {
      rules.put(
          modernName,
          assertNotNull(
              rules.get(oldName), "Tried to register a mapping for unknown rule " + oldName));
    }

    private <T, U> void registerRemapped(
        String modernName,
        String oldRule,
        Class<T> ruleType,
        Function<U, T> get,
        Function<T, U> set) {
      @SuppressWarnings("unchecked")
      var oldRuleObj = (GameRule<U>) rules.get(oldRule);
      rules.put(
          modernName,
          new SpRemappedGameRule<>(
              modernName,
              assertNotNull(oldRuleObj, "Tried to register a mapping for unknown rule " + oldRule),
              ruleType,
              get,
              set));
    }
  }
}
