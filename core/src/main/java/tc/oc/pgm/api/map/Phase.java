package tc.oc.pgm.api.map;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.api.Permissions.DEFAULT;
import static tc.oc.pgm.api.Permissions.DEV;
import static tc.oc.pgm.api.Permissions.SETNEXT;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public enum Phase {
  PRODUCTION(DEFAULT.getName()),
  STAGING(SETNEXT),
  DEVELOPMENT(DEV);

  private final String permission;

  Phase(String permission) {
    this.permission = permission;
  }

  public Component toComponent() {
    return translatable("map.phase." + this.name().toLowerCase());
  }

  public static Phase of(String query) {
    query = query.toLowerCase(Locale.ROOT);
    for (Phase phase : Phase.values()) {
      if (phase.toString().toLowerCase(Locale.ROOT).startsWith(query)) {
        return phase;
      }
    }
    return null;
  }

  public static class Phases {
    private final EnumSet<Phase> phases;

    private Phases(EnumSet<Phase> phases) {
      this.phases = phases;
    }

    public boolean contains(Phase phase) {
      return this.phases.contains(phase);
    }

    public static Phases of(Phase phase) {
      return new Phases(phase == null ? EnumSet.noneOf(Phase.class) : EnumSet.of(phase));
    }

    public static Phases of(Collection<Phases> inputPhases) {
      EnumSet<Phase> phases = EnumSet.noneOf(Phase.class);
      for (Phases inputPhase : inputPhases) {
        phases.addAll(inputPhase.phases);
      }
      return new Phases(phases);
    }

    public static Phases allOf() {
      return new Phases(EnumSet.allOf(Phase.class));
    }

    public static Phases parse(CommandSender sender, Collection<Phases> inputPhases) {
      Phases aggregate = of(inputPhases);
      if (!aggregate.phases.isEmpty()) return aggregate;

      EnumSet<Phase> chosenPhases = EnumSet.noneOf(Phase.class);
      EnumSet.allOf(Phase.class).stream()
          .filter(p -> sender.hasPermission(p.permission))
          .forEach(chosenPhases::add);
      return new Phases(chosenPhases);
    }
  }
}
