package tc.oc.pgm.compass;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public interface CompassTarget {
  Filter getFilter();

  Component getName(Match match, MatchPlayer player);

  Optional<Location> getLocation(Match match, MatchPlayer player);

  Optional<CompassTargetResult> getResult(Match match, MatchPlayer player);
}
