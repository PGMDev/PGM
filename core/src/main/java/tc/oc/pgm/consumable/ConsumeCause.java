package tc.oc.pgm.consumable;

public enum ConsumeCause {
  EAT,
  RIGHT_CLICK,
  LEFT_CLICK,
  CLICK;

  public boolean triggersOn(ConsumeCause cause) {
    return cause == this || (this == CLICK && (cause == RIGHT_CLICK || cause == LEFT_CLICK));
  }
}
