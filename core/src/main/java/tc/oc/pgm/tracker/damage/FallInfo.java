package tc.oc.pgm.tracker.damage;

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
