package tc.oc.pgm.goals;

import lombok.Getter;

@Getter
public enum ShowOption {
  SHOW_MESSAGES("show-messages"),
  SHOW_EFFECTS("show-effects"),
  SHOW_INFO("show-info"),
  SHOW_SIDEBAR("show-sidebar"),
  STATS("stats");

  private final String name;

  ShowOption(String name) {
    this.name = name;
  }
}
