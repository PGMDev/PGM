package tc.oc.pgm.platform.modern.modules.gamerules;

import static java.util.Map.entry;
import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.GameRules;
import org.bukkit.Registry;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.util.bukkit.GameRule;
import tc.oc.pgm.util.platform.Supports;

@Supports(PAPER)
@NullMarked
public class ModernGameRules implements tc.oc.pgm.util.bukkit.GameRules.GameRuleRegistry {
  private static final @Unmodifiable Map<String, GameRule<?>> GAME_RULES = new Builder().build();

  @Override
  public @Unmodifiable Map<String, GameRule<?>> getGameRules() {
    return GAME_RULES;
  }

  private static class Builder {
    private final Map<String, GameRule<?>> rules = new HashMap<>();
    private final Map<org.bukkit.GameRule<?>, ModernBukkitGameRule<?>> modernGameRules =
        Registry.GAME_RULE.stream()
            .map(rule -> entry(rule, new ModernBukkitGameRule<>(rule)))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    @SuppressWarnings({"removal", "UnstableApiUsage"})
    private @Unmodifiable Map<String, GameRule<?>> build() {
      modernGameRules.forEach((handle, rule) -> rules.put(handle.getKey().getKey(), rule));

      // List taken from DataConverter V4658 schema.
      // Entries that have been removed without a replacement will appear as unsupported but are
      // left here for posterity.
      // Remapped entries *that transform* the value are marked as remapped.
      // Remapped entries that do not transform the value in any way are re-pointed to their modern
      // equivalent.
      registerRemapped("doFireTick", org.bukkit.GameRule.DO_FIRE_TICK);
      registerRemapped(
          "allowFireTicksAwayFromPlayer", org.bukkit.GameRule.ALLOW_FIRE_TICKS_AWAY_FROM_PLAYER);
      // registerNoop("spawnChunkRadius")
      // registerNoop("entitiesWithPassengersCanUsePortals")
      registerLegacy(
          "allowEnteringUsingNetherPortals", GameRules.ALLOW_ENTERING_NETHER_USING_PORTALS);
      registerLegacy("announceAdvancements", GameRules.SHOW_ADVANCEMENT_MESSAGES);
      registerLegacy("blockExplosionDropDecay", GameRules.BLOCK_EXPLOSION_DROP_DECAY);
      registerLegacy("commandBlockOutput", GameRules.COMMAND_BLOCK_OUTPUT);
      registerLegacy("enableCommandBlocks", GameRules.COMMAND_BLOCKS_WORK);
      registerLegacy("commandBlocksEnabled", GameRules.COMMAND_BLOCKS_WORK);
      registerLegacy("commandModificationBlockLimit", GameRules.MAX_BLOCK_MODIFICATIONS);
      registerRemapped(
          "disableElytraMovementCheck", org.bukkit.GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK);
      registerRemapped(
          "disablePlayerMovementCheck", org.bukkit.GameRule.DISABLE_PLAYER_MOVEMENT_CHECK);
      registerRemapped("disableRaids", org.bukkit.GameRule.DISABLE_RAIDS);
      registerLegacy("doDaylightCycle", GameRules.ADVANCE_TIME);
      registerLegacy("doEntityDrops", GameRules.ENTITY_DROPS);
      registerLegacy("doImmediateRespawn", GameRules.IMMEDIATE_RESPAWN);
      registerLegacy("doInsomnia", GameRules.SPAWN_PHANTOMS);
      registerLegacy("doLimitedCrafting", GameRules.LIMITED_CRAFTING);
      registerLegacy("doMobLoot", GameRules.MOB_DROPS);
      registerLegacy("doMobSpawning", GameRules.SPAWN_MOBS);
      registerLegacy("doPatrolSpawning", GameRules.SPAWN_PATROLS);
      registerLegacy("doTileDrops", GameRules.BLOCK_DROPS);
      registerLegacy("doTraderSpawning", GameRules.SPAWN_WANDERING_TRADERS);
      registerLegacy("doVinesSpread", GameRules.SPREAD_VINES);
      registerLegacy("doWardenSpawning", GameRules.SPAWN_WARDENS);
      registerLegacy("doWeatherCycle", GameRules.ADVANCE_WEATHER);
      registerLegacy("drowningDamage", GameRules.DROWNING_DAMAGE);
      registerLegacy("enderPearlsVanishOnDeath", GameRules.ENDER_PEARLS_VANISH_ON_DEATH);
      registerLegacy("fallDamage", GameRules.FALL_DAMAGE);
      registerLegacy("fireDamage", GameRules.FIRE_DAMAGE);
      registerLegacy("forgiveDeadPlayers", GameRules.FORGIVE_DEAD_PLAYERS);
      registerLegacy("freezeDamage", GameRules.FREEZE_DAMAGE);
      registerLegacy("globalSoundEvents", GameRules.GLOBAL_SOUND_EVENTS);
      registerLegacy("keepInventory", GameRules.KEEP_INVENTORY);
      registerLegacy("lavaSourceConversion", GameRules.LAVA_SOURCE_CONVERSION);
      registerLegacy("locatorBar", GameRules.LOCATOR_BAR);
      registerLegacy("logAdminCommands", GameRules.LOG_ADMIN_COMMANDS);
      registerLegacy("maxCommandChainLength", GameRules.MAX_COMMAND_SEQUENCE_LENGTH);
      registerLegacy("maxCommandForkCount", GameRules.MAX_COMMAND_FORKS);
      registerLegacy("maxEntityCramming", GameRules.MAX_ENTITY_CRAMMING);
      registerLegacy("minecartMaxSpeed", GameRules.MAX_MINECART_SPEED);
      registerLegacy("mobExplosionDropDecay", GameRules.MOB_EXPLOSION_DROP_DECAY);
      registerLegacy("mobGriefing", GameRules.MOB_GRIEFING);
      registerLegacy("naturalRegeneration", GameRules.NATURAL_HEALTH_REGENERATION);
      registerLegacy(
          "playersNetherPortalCreativeDelay", GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY);
      registerLegacy(
          "playersNetherPortalDefaultDelay", GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY);
      registerLegacy("playersSleepingPercentage", GameRules.PLAYERS_SLEEPING_PERCENTAGE);
      registerLegacy("projectilesCanBreakBlocks", GameRules.PROJECTILES_CAN_BREAK_BLOCKS);
      // registerLegacy("pvp", GameRules.PVP); // populated by the registry
      registerLegacy("randomTickSpeed", GameRules.RANDOM_TICK_SPEED);
      registerLegacy("reducedDebugInfo", GameRules.REDUCED_DEBUG_INFO);
      registerLegacy("sendCommandFeedback", GameRules.SEND_COMMAND_FEEDBACK);
      registerLegacy("showDeathMessages", GameRules.SHOW_DEATH_MESSAGES);
      registerLegacy("snowAccumulationHeight", GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);
      registerLegacy("spawnMonsters", GameRules.SPAWN_MONSTERS);
      registerLegacy("spawnRadius", GameRules.RESPAWN_RADIUS);
      registerLegacy("spawnerBlocksEnabled", GameRules.SPAWNER_BLOCKS_WORK);
      registerLegacy("spectatorsGenerateChunks", GameRules.SPECTATORS_GENERATE_CHUNKS);
      registerLegacy("tntExplodes", GameRules.TNT_EXPLODES);
      registerLegacy("tntExplosionDropDecay", GameRules.TNT_EXPLOSION_DROP_DECAY);
      registerLegacy("universalAnger", GameRules.UNIVERSAL_ANGER);
      registerLegacy("waterSourceConversion", GameRules.WATER_SOURCE_CONVERSION);

      return Map.copyOf(rules);
    }

    private void registerLegacy(String oldName, org.bukkit.GameRule<?> handle) {
      rules.put(
          oldName,
          assertNotNull(
              modernGameRules.get(handle), "Using a game rule outside of game rule registry"));
    }

    private void registerRemapped(String oldName, org.bukkit.GameRule<?> handle) {
      rules.put(oldName, new ModernRemappedGameRule<>(handle, oldName));
    }
  }
}
