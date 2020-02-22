package tc.oc.pgm.gamerules;

public enum GameRule {
  DO_FIRE_TICK("doFireTick"),
  DO_MOB_LOOT("doMobLoot"),
  DO_TILE_DROPS("doTileDrops"),
  MOB_GRIEFING("mobGriefing"),
  NATURAL_REGENERATION("naturalRegeneration"),
  DO_DAYLIGHT_CYCLE("doDaylightCycle");

  private String value;

  GameRule(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }

  public static GameRule forName(String query) {
    for (GameRule gamerule : values()) {
      if (gamerule.getValue().equalsIgnoreCase(query)) {
        return gamerule;
      }
    }

    return null;
  }
}
