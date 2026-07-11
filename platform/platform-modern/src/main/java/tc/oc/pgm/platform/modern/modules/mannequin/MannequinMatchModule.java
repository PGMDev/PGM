package tc.oc.pgm.platform.modern.modules.mannequin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.bukkit.Location;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import java.util.Map;

public class MannequinMatchModule implements MatchModule {

  private final Match match;
  private final Map<String, MannequinDefinition> mannequinDefinitions;
  private final SetMultimap<String, Mannequin> instances = HashMultimap.create();

  public MannequinMatchModule(Match match, Map<String, MannequinDefinition> mannequinDefinitions) {
    this.match = match;
    this.mannequinDefinitions = mannequinDefinitions;
  }

  public void spawn(String id, Location origin) {
    MannequinDefinition mannequinDefinition = mannequinDefinitions.get(id);
    if (mannequinDefinition == null) return;
    instances.put(id, Mannequin.spawn(id, origin, mannequinDefinition));
  }

  public void despawn(String id) {
    instances.removeAll(id).forEach(Mannequin::despawn);
  }
}
