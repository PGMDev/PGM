package tc.oc.pgm.score;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.blitz.BlitzModule;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLFluentParser;
import tc.oc.pgm.util.xml.XMLUtils;

public class ScoreModule implements MapModule<ScoreMatchModule> {
  private static final MapTag SCORE_TAG = new MapTag("deathmatch", Gamemode.DEATHMATCH, false);
  private static final MapTag BOX_TAG = new MapTag("scorebox", "Scorebox");

  private final @NotNull ScoreConfig config;
  private final @NotNull Set<ScoreBoxDefinition> scoreBoxDefinitions;

  public ScoreModule(
      @NotNull ScoreConfig config, @NotNull Set<ScoreBoxDefinition> scoreBoxDefinitions) {
    assertNotNull(config, "score config");
    assertNotNull(scoreBoxDefinitions, "score box factories");

    this.config = config;
    this.scoreBoxDefinitions = scoreBoxDefinitions;
  }

  @NotNull
  public ScoreConfig getConfig() {
    return config;
  }

  @Override
  public Collection<MapTag> getTags() {
    ImmutableList.Builder<MapTag> builder = ImmutableList.builder();
    if (config.killScore() != 0 || config.deathScore() != 0) builder.add(SCORE_TAG);
    if (!scoreBoxDefinitions.isEmpty()) builder.add(BOX_TAG);
    return builder.build();
  }

  @Override
  public ScoreMatchModule createMatchModule(Match match) {
    ImmutableSet.Builder<ScoreBox> scoreBoxes = ImmutableSet.builder();
    for (ScoreBoxDefinition factory : this.scoreBoxDefinitions) {
      scoreBoxes.add(factory.createScoreBox(match));
    }

    return new ScoreMatchModule(match, this.config, scoreBoxes.build());
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
      var parser = factory.getParser();

      List<Element> scoreElements = doc.getRootElement().getChildren("score");
      if (scoreElements.isEmpty()) {
        return null;
      }

      RegionParser regionParser = factory.getRegions();
      int scoreLimit = -1;
      int deathScore = 0;
      int killScore = 0;
      int mercyLimit = -1;
      int mercyLimitMin = -1;
      var display = ScoreConfig.Display.NUMERICAL;
      Filter sbFilter = StaticFilter.ALLOW;
      ImmutableSet.Builder<ScoreBoxDefinition> scoreBoxes = ImmutableSet.builder();

      for (Element el : scoreElements) {
        scoreLimit = parser.parseInt(el, "limit").optional(-1);

        // For backwards compatibility, default kill/death points to 1 if proto is old and <king/>
        // tag is not present
        int defPoints = proto.isOlderThan(MapProtos.DEFAULT_SCORES_TO_ZERO) ? 1 : 0;
        int defaultScore = el.getChild("king") == null ? defPoints : 0;
        deathScore = parser.parseInt(el, "deaths").child().optional(defaultScore);
        killScore = parser.parseInt(el, "kills").child().optional(defaultScore);

        Element mercyEl = XMLUtils.getUniqueChild(el, "mercy");
        if (mercyEl != null) {
          mercyLimit = parser.parseInt(mercyEl).optional(-1);
          mercyLimitMin = parser.parseInt(mercyEl, "min").attr().optional(-1);
        }

        display = parser.parseEnum(ScoreConfig.Display.class, el, "display").optional(display);

        sbFilter = parser.filter(el, "scoreboard-filter").dynamic(Party.class).orAllow();

        for (Element scoreBoxEl : el.getChildren("box")) {
          int points = parser.parseInt(scoreBoxEl, "value", "points").attr().optional(defPoints);

          Filter filter = parser.filter(scoreBoxEl, "filter").orAllow();
          ImmutableMap<MaterialMatcher, Double> redeemables;
          Region region;

          if (proto.isOlderThan(MapProtos.MODULE_SUBELEMENT_VERSION)) {
            region = regionParser.parseChildren(scoreBoxEl);
            redeemables = ImmutableMap.of();
          } else {
            region = regionParser.parseRequiredRegionProperty(scoreBoxEl, "region");
            redeemables = parseRedeemables(scoreBoxEl.getChild("redeemables"), parser);
          }
          boolean silent = parser.parseBool(scoreBoxEl, "silent").attr().orFalse();

          scoreBoxes.add(new ScoreBoxDefinition(region, points, filter, redeemables, silent));
        }
      }

      var config = new ScoreConfig(
          scoreLimit, deathScore, killScore, mercyLimit, mercyLimitMin, display, sbFilter);
      return new ScoreModule(config, scoreBoxes.build());
    }

    private static ImmutableMap<MaterialMatcher, Double> parseRedeemables(
        Element redeemablesEl, XMLFluentParser parser) throws InvalidXMLException {
      if (redeemablesEl == null) return ImmutableMap.of();
      var redeemables = ImmutableMap.<MaterialMatcher, Double>builder();
      for (Element elItem : redeemablesEl.getChildren("item")) {
        double points = parser.parseDouble(elItem, "points").attr().optional(1d);
        redeemables.put(MaterialMatcher.parse(elItem), points);
      }
      return redeemables.build();
    }
  }
}
