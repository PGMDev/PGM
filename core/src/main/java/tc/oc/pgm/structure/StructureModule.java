package tc.oc.pgm.structure;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
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
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.parse.DynamicFilterValidation;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.snapshot.SnapshotMatchModule;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class StructureModule implements MapModule<StructureMatchModule> {

  private final Map<String, StructureDefinition> structures;
  private final List<DynamicStructureDefinition> dynamics;

  public StructureModule(
      Map<String, StructureDefinition> structures, List<DynamicStructureDefinition> dynamics) {
    this.structures = structures;
    this.dynamics = dynamics;
  }

  @Nullable
  @Override
  public StructureMatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new StructureMatchModule(match, this.structures, this.dynamics);
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
      Version proto = factory.getProto();
      if (proto.isOlderThan(MapProtos.FILTER_FEATURES)) return null;

      FilterParser filters = factory.getFilters();
      RegionParser regions = factory.getRegions();

      final Map<String, StructureDefinition> structures = new HashMap<>();
      for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "structures", "structure")) {
        final StructureDefinition definition =
            new StructureDefinition(
                XMLUtils.getRequiredAttribute(el, "id").getValue(),
                XMLUtils.parseVector(el.getAttribute("origin"), (Vector) null),
                regions.parseProperty(el, "region", BlockBoundedValidation.INSTANCE),
                XMLUtils.parseBoolean(el.getAttribute("air"), false),
                XMLUtils.parseBoolean(el.getAttribute("clear"), true));

        structures.put(definition.getId(), definition);
        factory.getFeatures().addFeature(el, definition);
      }

      final List<DynamicStructureDefinition> dynamics = new ArrayList<>();
      for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "structures", "dynamic")) {
        String id = el.getAttributeValue("id");
        if (id == null) id = UUID.randomUUID().toString();

        BlockVector position = XMLUtils.parseBlockVector(Node.fromAttr(el, "location"));
        BlockVector offset = XMLUtils.parseBlockVector(Node.fromAttr(el, "offset"));

        if (position != null && offset != null)
          throw new InvalidXMLException(
              "attributes 'location' and 'offset' cannot be used together", el);

        final StructureDefinition structure =
            structures.get(XMLUtils.getRequiredAttribute(el, "structure").getValue());

        Filter trigger, filter;
        if (proto.isOlderThan(MapProtos.DYNAMIC_FILTERS)) {
          // Legacy maps use "filter" as their trigger
          trigger = filters.parseRequiredProperty(el, "filter", DynamicFilterValidation.MATCH);
          filter = StaticFilter.ALLOW;
        } else {
          trigger = filters.parseRequiredProperty(el, "trigger", DynamicFilterValidation.MATCH);
          filter = filters.parseProperty(el, "filter", StaticFilter.ALLOW);
        }

        DynamicStructureDefinition definition =
            new DynamicStructureDefinition(id, structure, trigger, filter, position, offset);
        dynamics.add(definition);
        factory.getFeatures().addFeature(el, definition);
      }

      return new StructureModule(structures, dynamics);
    }
  }
}
