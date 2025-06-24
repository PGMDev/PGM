package tc.oc.pgm.api.map;

public enum Gamemode {
  ARCADE("arcade", "Arcade", "Arcade"),
  ATTACK_DEFEND("ad", "Attack/Defend", "A/D"),
  BEDWARS("bedwars", "Bed Wars", "Bed Wars"),
  BLITZ("blitz", "Blitz", "Blitz"),
  BLITZ_RAGE("br", "Blitz: Rage", "Blitz: Rage"),
  BRIDGE("bridge", "Bridge", "Bridge"),
  CAPTURE_THE_FLAG("ctf", "Capture the Flag", "CTF"),
  CAPTURE_THE_WOOL("ctw", "Capture the Wool", "CTW"),
  CONTROL_THE_POINT("cp", "Control the Point", "CP"),
  DEATHMATCH("tdm", "Deathmatch", "TDM"),
  DESTROY_THE_CORE("dtc", "Destroy the Core", "DTC"),
  DESTROY_THE_MONUMENT("dtm", "Destroy the Monument", "DTM"),
  FIVE_CONTROL_POINT("5cp", "5 Control Points", "5CP"),
  FLAG_FOOTBALL("ffb", "Flag Football", "FFB"),
  FREE_FOR_ALL("ffa", "Free for All", "FFA"),
  INFECTION("infection", "Infection", "Infection"),
  KING_OF_THE_FLAG("kotf", "King of the Flag", "KotF"),
  KING_OF_THE_HILL("koth", "King of the Hill", "KotH"),
  MIXED("mixed", "Mixed", "Mixed"),
  PARKOUR("parkour", "Parkour", "Parkour"),
  PAYLOAD("payload", "Payload", "Payload"),
  RACE_FOR_WOOL("rfw", "Race for Wool", "RFW"),
  RAGE("rage", "Rage", "Rage"),
  SCOREBOX("scorebox", "Scorebox", "Scorebox"),
  SKYWARS("skywars", "Skywars", "Skywars"),
  SURVIVAL_GAMES("sg", "Survival Games", "SG");

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
