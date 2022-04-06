package tc.oc.pgm.api.map;

public enum Gamemode {
  ATTACK_DEFEND("ad"),
  ARCADE("arcade"),
  BLITZ("blitz"),
  BLITZ_RAGE("br"),
  CAPTURE_THE_FLAG("ctf"),
  CONTROL_THE_POINT("cp"),
  CAPTURE_THE_WOOL("ctw"),
  DESTROY_THE_CORE("dtc"),
  DESTROY_THE_MONUMENT("dtm"),
  FREE_FOR_ALL("ffa"),
  FLAG_FOOTBALL("ffb"),
  KING_OF_THE_HILL("koth"),
  KING_OF_THE_FLAG("kotf"),
  MIXED("mixed"),
  RAGE("rage"),
  RACE_FOR_WOOL("rfw"),
  SCOREBOX("scorebox"),
  DEATHMATCH("tdm");

  private final String id;

  Gamemode(String id) {
    this.id = id;
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
}
