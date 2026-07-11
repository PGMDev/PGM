package tc.oc.pgm.platform.modern.modules.mannequin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Map;
import org.bukkit.Location;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;

public class MannequinMatchModule implements MatchModule {

  private final Match match;
  private final Map<String, MannequinDefinition> mannequinDefinitions;
  private final SetMultimap<String, Mannequin> instances = HashMultimap.create();

  public MannequinMatchModule(Match match, Map<String, MannequinDefinition> mannequinDefinitions) {
    this.match = match;
    this.mannequinDefinitions = mannequinDefinitions;
  }

  public void spawn(MannequinDefinition definition, double x, double y, double z) {
    Location origin = new Location(match.getWorld(), x, y, z);
    instances.put(definition.getId(), Mannequin.spawn(definition.getId(), origin, definition));
  }

  public void despawn(String id) {
    instances.removeAll(id).forEach(Mannequin::despawn);
  }
}
