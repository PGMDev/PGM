package tc.oc.pgm.damage;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.action.ActionModule;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class DamageModule implements MapModule<DamageMatchModule> {

  private final List<Filter> filters;
  private final Action<? super MatchPlayer> attackerAction;
  private final Action<? super MatchPlayer> victimAction;

  public DamageModule(
      List<Filter> filters,
      Action<? super MatchPlayer> attackerAction,
      Action<? super MatchPlayer> victimAction) {
    this.filters = filters;
    this.attackerAction = attackerAction;
    this.victimAction = victimAction;
  }

  @Override
  public DamageMatchModule createMatchModule(Match match) {
    return new DamageMatchModule(match, filters, attackerAction, victimAction);
  }

  public static class Factory implements MapModuleFactory<DamageModule> {

    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(ActionModule.class);
    }

    @Override
    public DamageModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      var parser = factory.getParser();

      List<Filter> filters = new ArrayList<>();
      Action<? super MatchPlayer> attackerAction = null;
      Action<? super MatchPlayer> victimAction = null;

      for (Element elDamage : doc.getRootElement().getChildren("damage")) {
        attackerAction = parser
            .action(MatchPlayer.class, elDamage, "attacker-action")
            .attr()
            .optional(attackerAction);
        victimAction = parser
            .action(MatchPlayer.class, elDamage, "victim-action")
            .attr()
            .optional(victimAction);

        for (Element elFilter : elDamage.getChildren()) {
          filters.add(parser.filter(elFilter).required());
        }
      }

      // Always load due to misc damage handling
      return new DamageModule(ImmutableList.copyOf(filters), attackerAction, victimAction);
    }
  }
}
