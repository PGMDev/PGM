package tc.oc.pgm.integration;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.integration.RequestIntegration;
import tc.oc.pgm.api.map.MapInfo;

public class RequestIntegrationImpl implements RequestIntegration {

  @Override
  public List<Component> getExtraMatchInfoLines(MapInfo map) {
    return Lists.newArrayList();
  }

  @Override
  public boolean isSponsor(MapInfo map) {
    return false;
  }
}
