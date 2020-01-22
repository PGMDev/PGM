package tc.oc.pgm.damage;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.filters.Filter;
import tc.oc.xml.InvalidXMLException;

public class DamageModule implements MapModule {

  private final List<Filter> filters;

  public DamageModule(List<Filter> filters) {
    this.filters = filters;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new DamageMatchModule(match, filters);
  }

  public static class Factory implements MapModuleFactory<DamageModule> {
    @Override
    public DamageModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<Filter> filters = new ArrayList<>();

      for (Element elDamage : doc.getRootElement().getChildren("damage")) {
        for (Element elFilter : elDamage.getChildren()) {
          filters.add(factory.getFilters().parse(elFilter));
        }
      }

      // Always load due to misc damage handling
      return new DamageModule(ImmutableList.copyOf(filters));
    }
  }
}
