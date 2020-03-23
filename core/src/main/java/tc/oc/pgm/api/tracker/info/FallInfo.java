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

  From getFrom();

  To getTo();
}
