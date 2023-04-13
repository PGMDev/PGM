package tc.oc.pgm.modes;

import static tc.oc.pgm.api.map.MapProtos.MODES_IMPLEMENTATION_VERSION;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.Collection;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.material.MaterialData;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.parse.DynamicFilterValidation;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.util.text.TextParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ObjectiveModesModule implements MapModule<ObjectiveModesMatchModule> {

  private final ImmutableList<Mode> modes;
  public static final Duration DEFAULT_SHOW_BEFORE = Duration.ofSeconds(60L);

  private ObjectiveModesModule(ImmutableList<Mode> modes) {
    this.modes = modes;
  }

  @Nullable
  @Override
  public Collection<Class<? extends MatchModule>> getHardDependencies() {
    return ImmutableList.of(FilterMatchModule.class);
  }

  @Override
  public Collection<Class<? extends MatchModule>> getSoftDependencies() {
    return ImmutableList.of(GoalMatchModule.class);
  }

  @Override
  public ObjectiveModesMatchModule createMatchModule(Match match) {
    return new ObjectiveModesMatchModule(match, this.modes);
  }

  public static class Factory implements MapModuleFactory<ObjectiveModesModule> {
    @Override
    public ObjectiveModesModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      if (factory.getProto().isOlderThan(MODES_IMPLEMENTATION_VERSION)) {
        return null;
      }

      ImmutableList.Builder<Mode> parsedModes = ImmutableList.builder();

      if (doc.getRootElement().getChild("modes") == null) {
        return null;
      }

      for (Element modeEl : XMLUtils.flattenElements(doc.getRootElement(), "modes", "mode")) {
        String id = modeEl.getAttributeValue("id");
        if (modeEl.getAttributeValue("after") == null) {
          throw new InvalidXMLException("No period has been specified", modeEl);
        }

        MaterialData material =
            XMLUtils.parseBlockMaterialData(Node.fromRequiredAttr(modeEl, "material"));
        Duration after = TextParser.parseDuration(modeEl.getAttributeValue("after"));
        Filter filter =
            factory.getFilters().parseProperty(modeEl, "filter", DynamicFilterValidation.MATCH);
        String name = modeEl.getAttributeValue("name");
        if (name != null) {
          name = ChatColor.translateAlternateColorCodes('`', name);
        }

        String showBeforeRaw = modeEl.getAttributeValue("show-before");
        Duration showBefore =
            showBeforeRaw != null ? TextParser.parseDuration(showBeforeRaw) : DEFAULT_SHOW_BEFORE;

        // Legacy
        boolean legacyShowBossBar = XMLUtils.parseBoolean(modeEl.getAttribute("boss-bar"), true);
        if (!legacyShowBossBar) showBefore = Duration.ZERO;

        // Autogenerate a unique id, required for /mode start
        if (id == null) {
          String legacyName = name != null ? name : ModeUtils.formatMaterial(material);
          id = makeUniqueId(legacyName, factory.getFeatures());
        }

        Mode mode = new Mode(id, material, after, filter, name, showBefore);
        parsedModes.add(mode);
        factory.getFeatures().addFeature(modeEl, mode);
      }

      return new ObjectiveModesModule(parsedModes.build());
    }

    private String makeUniqueId(String name, FeatureDefinitionContext features) {
      String baseId = "mode-" + Mode.makeId(name);
      if (!features.contains(baseId)) return baseId;

      for (int i = 2; ; i++) {
        String newId = baseId + "-" + i;
        if (!features.contains(newId)) return newId;
      }
    }
  }
}
