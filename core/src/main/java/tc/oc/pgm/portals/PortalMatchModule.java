package tc.oc.pgm.portals;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.FilterMatchModule;

@ListenerScope(MatchScope.LOADED)
public class PortalMatchModule implements MatchModule, Listener, Tickable {

  private final Match match;
  protected final Set<Portal> portals;

  static final Set<MatchPlayer> teleportedPlayers = new HashSet<>();

  public PortalMatchModule(Match match, Set<Portal> portals) {
    this.match = match;
    this.portals = portals;
  }

  @Override
  public void load() throws ModuleLoadException {
    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);

    portals.forEach(portal -> portal.load(fmm));
  }

  public static boolean teleported(MatchPlayer player) {
    return !teleportedPlayers.add(player);
  }

  @Override
  public void tick(Match match, Tick tick) {
    teleportedPlayers.clear();
  }
}
