package tc.oc.pgm.api.map;

public enum Gamemode {
  ATTACK_DEFEND("ad", "Attack/Defend"),
  ARCADE("arcade", "Arcade"),
  BLITZ("blitz", "Blitz"),
  BLITZ_RAGE("br", "Blitz: Rage"),
  CAPTURE_THE_FLAG("ctf", "Capture the Flag"),
  CONTROL_THE_POINT("cp", "Control the Point"),
  CAPTURE_THE_WOOL("ctw", "Capture the Wool"),
  DESTROY_THE_CORE("dtc", "Destroy the Core"),
  DESTROY_THE_MONUMENT("dtm", "Destroy the Monument"),
  FREE_FOR_ALL("ffa", "Free For All"),
  FLAG_FOOTBALL("ffb", "Flag Football"),
  KING_OF_THE_HILL("koth", "King of the Hill"),
  KING_OF_THE_FLAG("kotf", "King of the Flag"),
  MIXED("mixed", "Mixed"),
  RAGE("rage", "Rage"),
  RACE_FOR_WOOL("rfw", "Race for Wool"),
  SCOREBOX("scorebox", "Scorebox"),
  DEATHMATCH("tdm", "Deathmatch");

  private final String id;
  private final String name;
  private final MapTag mapTag;

  Gamemode(String id, String name) {
    this.id = id;
    this.name = name;
    this.mapTag = new MapTag(id, name, true, false);
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public MapTag getMapTag() {
    return mapTag;
  }
}
