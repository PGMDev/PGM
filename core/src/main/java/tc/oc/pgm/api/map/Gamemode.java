package tc.oc.pgm.api.map;

public enum Gamemode {
  ATTACK_DEFEND("ad", "Attack/Defend", "A/D"),
  ARCADE("arcade", "Arcade", "Arcade"),
  BEDWARS("bedwars", "Bed Wars", "Bed Wars"),
  BLITZ("blitz", "Blitz", "Blitz"),
  BLITZ_RAGE("br", "Blitz: Rage", "Blitz: Rage"),
  BRIDGE("bridge", "Bridge", "Bridge"),
  CAPTURE_THE_FLAG("ctf", "Capture the Flag", "CTF"),
  CONTROL_THE_POINT("cp", "Control the Point", "CP"),
  CAPTURE_THE_WOOL("ctw", "Capture the Wool", "CTW"),
  DESTROY_THE_CORE("dtc", "Destroy the Core", "DTC"),
  DESTROY_THE_MONUMENT("dtm", "Destroy the Monument", "DTM"),
  FREE_FOR_ALL("ffa", "Free for All", "FFA"),
  FLAG_FOOTBALL("ffb", "Flag Football", "FFB"),
  INFECTION("infection", "Infection", "Infection"),
  KING_OF_THE_HILL("koth", "King of the Hill", "KotH"),
  KING_OF_THE_FLAG("kotf", "King of the Flag", "KotF"),
  MIXED("mixed", "Mixed", "Mixed"),
  PAYLOAD("payload", "Payload", "Payload"),
  RAGE("rage", "Rage", "Rage"),
  RACE_FOR_WOOL("rfw", "Race for Wool", "RFW"),
  SCOREBOX("scorebox", "Scorebox", "Scorebox"),
  SKYWARS("skywars", "Skywars", "Skywars"),
  SURVIVAL_GAMES("sg", "Survival Games", "SG"),
  DEATHMATCH("tdm", "Deathmatch", "TDM"),
  OBJECTIVES("obj", "Objectives", "Objectives");

  private final String id;

  private final String name;
  private final String acronym;

  Gamemode(String id, String name, String acronym) {
    this.id = id;
    this.name = name;
    this.acronym = acronym;
  }

  public static Gamemode byId(String gamemodeId) {
    for (Gamemode gamemode : Gamemode.values()) {
      if (gamemode.getId().equalsIgnoreCase(gamemodeId)) {
        return gamemode;
      }
    }
    return null;
  }

  public String getId() {
    return id;
  }

  public String getFullName() {
    return name;
  }

  public String getAcronym() {
    return acronym;
  }
}
