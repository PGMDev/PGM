package tc.oc.pgm.stats;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLFluentParser;
import tc.oc.pgm.util.xml.XMLUtils;
import tc.oc.pgm.variables.VariablesModule;

public class StatsModule implements MapModule<StatsMatchModule> {
  private final ImmutableList<StatType.OfFormula> formulaStats;

  private StatsModule(ImmutableList<StatType.OfFormula> formulaStats) {
    this.formulaStats = formulaStats;
  }

  @Override
  public @Nullable StatsMatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new StatsMatchModule(match, formulaStats);
  }

  public static class Factory implements MapModuleFactory<StatsModule> {

    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(VariablesModule.class);
    }

    @Override
    public StatsModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      XMLFluentParser parser = factory.getParser();
      ImmutableList.Builder<StatType.OfFormula> formulaStats = new ImmutableList.Builder<>();
      for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "stats", "stat")) {
        Component name = parser.component(el, "name").required();
        TextColor color = parser.textColor(el, "color").optional(NamedTextColor.GREEN);
        Formula<MatchPlayer> formulaDef =
            parser.formula(MatchPlayer.class, el, "value").required();
        formulaStats.add(new StatType.OfFormula(name, formulaDef, color));
      }
      return new StatsModule(formulaStats.build());
    }
  }
}
