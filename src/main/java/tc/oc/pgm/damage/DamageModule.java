package tc.oc.pgm.damage;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(name = "damage")
public class DamageModule extends MapModule {

  private final List<Filter> filters;

  public DamageModule(List<Filter> filters) {
    this.filters = filters;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new DamageMatchModule(match, filters);
  }

  public static DamageModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    List<Filter> filters = new ArrayList<>();

    for (Element elDamage : doc.getRootElement().getChildren("damage")) {
      for (Element elFilter : elDamage.getChildren()) {
        filters.add(context.getFilterParser().parse(elFilter));
      }
    }

    return new DamageModule(ImmutableList.copyOf(filters));
  }
}
