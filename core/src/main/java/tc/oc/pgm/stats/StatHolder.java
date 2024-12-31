package tc.oc.pgm.stats;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

public interface StatHolder {
  JoinConfiguration PIPE = separator(text("  |  "));
  JoinConfiguration SPACES = separator(text("  "));

  Number getStat(StatType type);

  default Component pipeSeparated(StatType... types) {
    return getComponent(PIPE, types);
  }

  default Component spaceSeparated(StatType... types) {
    return getComponent(SPACES, types);
  }

  default Component getComponent(JoinConfiguration joining, StatType... types) {
    Component[] children = new Component[types.length];
    for (int i = 0; i < types.length; i++) children[i] = types[i].component(this);
    return join(joining, children);
  }
}
