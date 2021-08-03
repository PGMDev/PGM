package tc.oc.pgm.api.map;

public enum GameRule {
  DO_DAYLIGHT_CYCLE("doDaylightCycle"),
  DO_FIRE_TICK("doFireTick"),
  DO_MOB_LOOT("doMobLoot"),
  DO_TILE_DROPS("doTileDrops"),
  MOB_GRIEFING("mobGriefing"),
  NATURAL_REGENERATION("naturalRegeneration");

  /* Unsupported Gamerules:
  doMobSpawning
  keepInventory
  commandBlockOutput
  logAdminCommands
  randomTickSpeed
  reducedDebugInfo
  sendCommandFeedback
  showDeathMessages
   */

  private final String id;

  GameRule(String id) {
    this.id = id;
  }

  public static GameRule byId(String gameRuleId) {
    for (GameRule gameRule : GameRule.values()) {
      if (gameRule.getId().equals(gameRuleId)) {
        return gameRule;
      }
    }
    return null;
  }

  public String getId() {
    return id;
  }
}
