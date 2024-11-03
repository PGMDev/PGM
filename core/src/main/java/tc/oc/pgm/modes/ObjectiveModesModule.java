package tc.oc.pgm.modes;

import static tc.oc.pgm.api.map.MapProtos.MODES_IMPLEMENTATION_VERSION;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.Collection;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.util.material.BlockMaterialData;
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

      var parser = factory.getParser();

      ImmutableList.Builder<Mode> parsedModes = ImmutableList.builder();

      if (doc.getRootElement().getChild("modes") == null) {
        return null;
      }

      for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "modes", "mode")) {
        var name = parser.string(el, "name").colored().orNull();
        var after = parser.duration(el, "after").required();
        var showBefore = parser.duration(el, "show-before").optional(DEFAULT_SHOW_BEFORE);
        if (!parser.parseBool(el, "boss-bar").orTrue()) showBefore = Duration.ZERO;
        var filter = parser.filter(el, "filter").dynamic(Match.class).orNull();

        var material = XMLUtils.parseBlockMaterialData(Node.fromAttr(el, "material"));
        var action = parser.action(Match.class, el, "action").orNull();

        if (material == null && (name == null || action == null))
          throw new InvalidXMLException("Expected either 'material', or 'name' & 'action'", el);

        // Autogenerate a unique id, required for /mode start
        String id = parser
            .string(el, "id")
            .optional(() -> makeUniqueId(name, material, factory.getFeatures()));

        Mode mode = new Mode(id, name, after, showBefore, filter, material, action);
        parsedModes.add(mode);
        factory.getFeatures().addFeature(el, mode);
      }

      return new ObjectiveModesModule(parsedModes.build());
    }

    private String makeUniqueId(
        String name, BlockMaterialData material, FeatureDefinitionContext features) {
      if (name == null) name = ModeUtils.formatMaterial(material);

      String baseId = "mode-" + Mode.makeId(name);
      if (!features.contains(baseId)) return baseId;

      for (int i = 2; ; i++) {
        String newId = baseId + "-" + i;
        if (!features.contains(newId)) return newId;
      }
    }
  }
}
