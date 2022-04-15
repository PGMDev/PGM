package tc.oc.pgm.goals;

public enum ObjectiveOption {
  SHOW_MESSAGES("show-messages"),
  SHOW_EFFECTS("show-effects"),
  SHOW_INFO("show-info"),
  SHOW_SIDEBAR("show-sidebar"),
  STATS("stats");

  private final String name;

  ObjectiveOption(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
