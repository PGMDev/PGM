package tc.oc.pgm.api.tracker.info;

public interface FallInfo extends DamageInfo, CauseInfo, RangedInfo {

  enum From {
    GROUND,
    LADDER,
    WATER
  }

  enum To {
    GROUND,
    LAVA,
    VOID
  }

  From from();

  To to();

  @Deprecated
  default From getFrom() {
    return from();
  }

  @Deprecated
  default To getTo() {
    return to();
  }
}
