package tc.oc.pgm.action;

public enum SoundType {
  CUSTOM("note.pling", 1f, 1f),
  TIP("mob.endermen.idle", 1f, 1.2f),
  ALERT("note.pling", 1f, 2f),
  PORTAL("mob.endermen.portal", 1f, 1f),
  SCORE("random.levelup", 1f, 1f),
  OBJECTIVE_FIREWORKS_FAR("fireworks.blast_far", 0.75f, 1f),
  OBJECTIVE_FIREWORKS_TWINKLE("fireworks.twinkle_far", 0.75f, 1f),
  OBJECTIVE_GOOD("portal.travel", 0.7f, 2f),
  OBJECTIVE_BAD("mob.blaze.death", 0.8f, 0.8f),
  OBJECTIVE_MODE("mob.zombie.remedy", 0.15f, 1.2f),
  DEATH_OWN("mob.irongolem.death", 1f, 1f),
  DEATH_OTHER("mob.irongolem.hit", 1f, 1f);

  private final String resource;
  private final float volume;
  private final float pitch;

  SoundType(String resource, float volume, float pitch) {
    this.resource = resource;
    this.volume = volume;
    this.pitch = pitch;
  }

  public String getResource() {
    return resource;
  }

  public float getVolume() {
    return volume;
  }

  public float getPitch() {
    return pitch;
  }
}
