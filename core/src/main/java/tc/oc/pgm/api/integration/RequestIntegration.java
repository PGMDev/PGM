package tc.oc.pgm.api.integration;

import java.util.List;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.map.MapInfo;

public interface RequestIntegration {

  public List<Component> getExtraMatchInfoLines(MapInfo map);

  public boolean isSponsor(MapInfo map);
}
