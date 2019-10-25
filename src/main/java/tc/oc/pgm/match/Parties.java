package tc.oc.pgm.match;

import javax.annotation.Nullable;

public class Parties {

  private Parties() {}

  public static boolean isNull(@Nullable Party team) {
    return team == null;
  }

  public static boolean isObserving(@Nullable Party team) {
    return team != null && team.isObserving();
  }

  public static boolean isObservingType(@Nullable Party team) {
    return team != null && team.getType() == Party.Type.Observing;
  }

  public static boolean isParticipating(@Nullable Party team) {
    return team != null && team.isParticipating();
  }

  public static boolean isParticipatingType(@Nullable Party team) {
    return team != null && team.getType() == Party.Type.Participating;
  }
}
