package tc.oc.pgm.score;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.blitz.BlitzModule;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.util.SemanticVersion;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class ScoreModule implements MapModule {
  public ScoreModule(@Nonnull ScoreConfig config, @Nonnull Set<ScoreBoxFactory> scoreBoxFactories) {
    Preconditions.checkNotNull(config, "score config");
    Preconditions.checkNotNull(scoreBoxFactories, "score box factories");

    this.config = config;
    this.scoreBoxFactories = scoreBoxFactories;
  }

  // FIXME: custom sidebar
  // private static final Component GAME =
  //    new PersonalizedTranslatable("match.scoreboard.scores.title");

  @Override
  public MatchModule createMatchModule(Match match) {
    ImmutableSet.Builder<ScoreBox> scoreBoxes = ImmutableSet.builder();
    for (ScoreBoxFactory factory : this.scoreBoxFactories) {
      scoreBoxes.add(factory.createScoreBox(match));
    }

    return new ScoreMatchModule(match, this.config, scoreBoxes.build());
  }

  private final @Nonnull ScoreConfig config;
  private final @Nonnull Set<ScoreBoxFactory> scoreBoxFactories;

  @Nonnull
  public ScoreConfig getConfig() {
    return config;
  }

  public static class Factory implements MapModuleFactory<ScoreModule> {
    @Override
    public Collection<Class<? extends MapModule>> getHardDependencies() {
      return ImmutableList.of(RegionModule.class, FilterModule.class);
    }

    @Override
    public Collection<Class<? extends MapModule>> getWeakDependencies() {
      return ImmutableList.of(BlitzModule.class); // GoalModule
    }

    @Override
    public ScoreModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      SemanticVersion proto = factory.getProto();

      List<Element> scoreElements = doc.getRootElement().getChildren("score");
      if (scoreElements.size() == 0) {
        return null;
      }

      RegionParser regionParser = factory.getRegions();
      ScoreConfig config = new ScoreConfig();
      ImmutableSet.Builder<ScoreBoxFactory> scoreBoxFactories = ImmutableSet.builder();

      for (Element scoreEl : scoreElements) {
        config.scoreLimit = XMLUtils.parseNumber(scoreEl.getChild("limit"), Integer.class, -1);

        // For backwards compatibility, default kill/death points to 1 if proto is old and <king/>
        // tag
        // is not present
        boolean scoreKillsByDefault =
            proto.isOlderThan(MapProtos.DEFAULT_SCORES_TO_ZERO) && scoreEl.getChild("king") == null;
        config.deathScore =
            XMLUtils.parseNumber(
                scoreEl.getChild("deaths"), Integer.class, scoreKillsByDefault ? 1 : 0);
        config.killScore =
            XMLUtils.parseNumber(
                scoreEl.getChild("kills"), Integer.class, scoreKillsByDefault ? 1 : 0);

        for (Element scoreBoxEl : scoreEl.getChildren("box")) {
          int points =
              XMLUtils.parseNumber(
                  Node.fromAttr(scoreBoxEl, "value", "points"),
                  Integer.class,
                  proto.isOlderThan(MapProtos.DEFAULT_SCORES_TO_ZERO) ? 1 : 0);

          Filter filter =
              factory.getFilters().parseFilterProperty(scoreBoxEl, "filter", StaticFilter.ALLOW);
          Map<SingleMaterialMatcher, Double> redeemables = new HashMap<>();
          Region region;

          if (proto.isOlderThan(MapProtos.MODULE_SUBELEMENT_VERSION)) {
            region = regionParser.parseChildren(scoreBoxEl);
          } else {
            region = regionParser.parseRequiredRegionProperty(scoreBoxEl, "region");

            Element elItems = scoreBoxEl.getChild("redeemables");
            if (elItems != null) {
              for (Element elItem : elItems.getChildren("item")) {
                redeemables.put(
                    XMLUtils.parseMaterialPattern(elItem),
                    XMLUtils.parseNumber(Node.fromAttr(elItem, "points"), Double.class, 1D));
              }
            }
          }

          scoreBoxFactories.add(
              new ScoreBoxFactory(region, points, filter, ImmutableMap.copyOf(redeemables)));
        }
      }
      return new ScoreModule(config, scoreBoxFactories.build());
    }
  }
}
