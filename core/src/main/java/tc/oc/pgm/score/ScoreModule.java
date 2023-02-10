package tc.oc.pgm.score;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.blitz.BlitzModule;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.parse.DynamicFilterValidation;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ScoreModule implements MapModule<ScoreMatchModule> {
  private static final MapTag SCORE_TAG =
      new MapTag("tdm", "deathmatch", "Deathmatch", true, false);
  private static final MapTag BOX_TAG = new MapTag("scorebox", "Scorebox", false, true);

  public ScoreModule(@NotNull ScoreConfig config, @NotNull Set<ScoreBoxFactory> scoreBoxFactories) {
    assertNotNull(config, "score config");
    assertNotNull(scoreBoxFactories, "score box factories");

    this.config = config;
    this.scoreBoxFactories = scoreBoxFactories;
  }

  @Override
  public Collection<MapTag> getTags() {
    ImmutableList.Builder<MapTag> builder = ImmutableList.builder();
    if (config.killScore != 0 || config.deathScore != 0) builder.add(SCORE_TAG);
    if (!scoreBoxFactories.isEmpty()) builder.add(BOX_TAG);
    return builder.build();
  }

  @Override
  public ScoreMatchModule createMatchModule(Match match) {
    ImmutableSet.Builder<ScoreBox> scoreBoxes = ImmutableSet.builder();
    for (ScoreBoxFactory factory : this.scoreBoxFactories) {
      scoreBoxes.add(factory.createScoreBox(match));
    }

    return new ScoreMatchModule(match, this.config, scoreBoxes.build());
  }

  private final @NotNull ScoreConfig config;
  private final @NotNull Set<ScoreBoxFactory> scoreBoxFactories;

  @NotNull
  public ScoreConfig getConfig() {
    return config;
  }

  public static class Factory implements MapModuleFactory<ScoreModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(RegionModule.class, FilterModule.class);
    }

    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(BlitzModule.class);
    }

    @Override
    public ScoreModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Version proto = factory.getProto();
      FilterParser filters = factory.getFilters();

      List<Element> scoreElements = doc.getRootElement().getChildren("score");
      if (scoreElements.size() == 0) {
        return null;
      }

      RegionParser regionParser = factory.getRegions();
      int scoreLimit = -1;
      int deathScore = 0;
      int killScore = 0;
      int mercyLimit = 0;
      int mercyLimitMin = 0;
      Filter scoreboardFilter = StaticFilter.ALLOW;
      ImmutableSet.Builder<ScoreBoxFactory> scoreBoxFactories = ImmutableSet.builder();

      for (Element scoreEl : scoreElements) {
        scoreLimit = XMLUtils.parseNumber(scoreEl.getChild("limit"), Integer.class, -1);

        Element mercyEl = XMLUtils.getUniqueChild(scoreEl, "mercy");
        if (mercyEl != null) {
          mercyLimit = XMLUtils.parseNumber(mercyEl, Integer.class, -1);
          mercyLimitMin = XMLUtils.parseNumber(Node.fromAttr(mercyEl, "min"), Integer.class, -1);
        }

        int defaultPoints = 0;
        if (proto.isOlderThan(MapProtos.DEFAULT_SCORES_TO_ZERO)
            && scoreEl.getChild("king") == null) {
          // For backwards compatibility, default kill/death points to 1 if proto is old and <king/>
          // tag is not present
          defaultPoints = 1;
        }
        deathScore = XMLUtils.parseNumber(scoreEl.getChild("deaths"), Integer.class, defaultPoints);
        killScore = XMLUtils.parseNumber(scoreEl.getChild("kills"), Integer.class, defaultPoints);

        scoreboardFilter =
            filters.parseProperty(
                scoreEl, "scoreboard-filter", StaticFilter.ALLOW, DynamicFilterValidation.PARTY);

        for (Element scoreBoxEl : scoreEl.getChildren("box")) {
          int points =
              XMLUtils.parseNumber(
                  Node.fromAttr(scoreBoxEl, "value", "points"),
                  Integer.class,
                  proto.isOlderThan(MapProtos.DEFAULT_SCORES_TO_ZERO) ? 1 : 0);

          Filter filter = filters.parseProperty(scoreBoxEl, "filter", StaticFilter.ALLOW);
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
          boolean silent = XMLUtils.parseBoolean(Node.fromAttr(scoreBoxEl, "silent"), false);

          scoreBoxFactories.add(
              new ScoreBoxFactory(
                  region, points, filter, ImmutableMap.copyOf(redeemables), silent));
        }
      }

      return new ScoreModule(
          new ScoreConfig(
              scoreLimit, deathScore, killScore, mercyLimit, mercyLimitMin, scoreboardFilter),
          scoreBoxFactories.build());
    }
  }
}
