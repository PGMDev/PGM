package tc.oc.pgm.structure;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.filters.dynamic.FilterMatchModule;
import tc.oc.pgm.regions.CuboidValidation;
import tc.oc.pgm.regions.RegionMatchModule;
import tc.oc.pgm.snapshot.SnapshotMatchModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class StructureModule implements MapModule<StructureMatchModule> {

  private final List<DynamicDefinition> dynamics;

  public StructureModule(List<DynamicDefinition> dynamics) {
    this.dynamics = dynamics;
  }

  @Nullable
  @Override
  public StructureMatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new StructureMatchModule(match, this.dynamics);
  }

  @Nullable
  @Override
  public Collection<Class<? extends MatchModule>> getHardDependencies() {
    return ImmutableList.of(FilterMatchModule.class, SnapshotMatchModule.class);
  }

  public static class Factory implements MapModuleFactory<StructureModule> {

    @Nullable
    @Override
    public StructureModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      if (factory.getProto().isOlderThan(MapProtos.FILTER_FEATURES)) return null; // TODO

      final Map<String, StructureDefinition> structures = new HashMap<>();
      for (Element elStruct :
          XMLUtils.flattenElements(doc.getRootElement(), "structures", "structure")) {
        final StructureDefinition definition =
            new StructureDefinition(
                elStruct.getAttribute("id").getValue(), // TODO Default?
                XMLUtils.parseVector(elStruct.getAttribute("origin"), (Vector) null),
                factory
                    .getRegions()
                    .parseRegionProperty(elStruct, CuboidValidation.INSTANCE, "region"),
                XMLUtils.parseBoolean(elStruct.getAttribute("air"), false),
                XMLUtils.parseBoolean(elStruct.getAttribute("clear"), true));

        structures.put(definition.getId(), definition);
        factory.getFeatures().addFeature(elStruct, definition);
      }

      final List<DynamicDefinition> dynamics = new LinkedList<>();
      for (Element elDynamic :
          XMLUtils.flattenElements(doc.getRootElement(), "structures", "dynamic")) {
        final @Nullable Attribute idAttr = elDynamic.getAttribute("id");
        final String id = idAttr != null ? idAttr.getValue() : UUID.randomUUID().toString();

        final @Nullable Attribute loc = elDynamic.getAttribute("location");
        final Vector position =
            loc != null ? XMLUtils.parseVector(Node.fromNullable(elDynamic), loc.getValue()) : null;

        final @Nullable Attribute off = elDynamic.getAttribute("offset");
        final Vector offset =
            off != null ? XMLUtils.parseVector(Node.fromNullable(elDynamic), off.getValue()) : null;

        if (position != null && offset != null) {
          throw new InvalidXMLException(
              "attributes 'location' and 'offset' cannot be used together", elDynamic);
        }

        final StructureDefinition structure =
            structures.get(elDynamic.getAttribute("structure").getValue());

        final FilterParser filterParser = factory.getFilters();
        final Filter trigger = filterParser.parseFilterProperty(elDynamic, "trigger", StaticFilter.ALLOW);
        final Filter filter = filterParser.parseFilterProperty(elDynamic, "filter", StaticFilter.ALLOW);

        final DynamicDefinition definition =
            new DynamicDefinition(id, structure, trigger, filter, position, offset);
        dynamics.add(definition);
        factory.getFeatures().addFeature(elDynamic, definition);
      }

      return new StructureModule(dynamics);
    }
  }
}
