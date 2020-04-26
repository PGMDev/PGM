package tc.oc.pgm.spawner;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.units.qual.A;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.regions.EverywhereRegion;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.renewable.RenewableModule;
import tc.oc.pgm.spawner.objects.SpawnerObjectEntity;
import tc.oc.pgm.spawner.objects.SpawnerObjectTNT;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class SpawnerModule implements MapModule {

    private final List<SpawnerDefinition> spawnerDefinitions = new ArrayList<>();

    @Override
    public MatchModule createMatchModule(Match match) {
        return new SpawnerMatchModule(match, this.spawnerDefinitions);
    }

    public static class Factory implements MapModuleFactory<SpawnerModule> {
        @Override
        public Collection<Class<? extends MapModule>> getWeakDependencies() {
            return ImmutableList.of(RegionModule.class, FilterModule.class);
        }

        @Override
        public SpawnerModule parse(MapFactory factory, Logger logger, Document doc)
                throws InvalidXMLException {
            SpawnerModule spawnerModule = new SpawnerModule();
            RegionParser regionParser = factory.getRegions();
            FilterParser filterParser = factory.getFilters();

            for (Element element :
                    XMLUtils.flattenElements(doc.getRootElement(), "spawners", "spawner")) {
               SpawnerDefinition spawnerDefinition = new SpawnerDefinition();
               spawnerDefinition.region = regionParser.parseRequiredRegionProperty(element, "region");
               spawnerDefinition.id = element.getAttributeValue("id");
               spawnerDefinition.count = XMLUtils.parseNumber(element.getAttribute("count"), Integer.class);
               Attribute delay = element.getAttribute("delay");
               Attribute minDelay = element.getAttribute("min-delay");
               Attribute maxDelay = element.getAttribute("max-delay");

               if ((minDelay != null || maxDelay != null) && delay != null) {
                   throw new InvalidXMLException("Attribute 'minDelay' and 'maxDelay' cannot be combined with 'delay'", element);
               }

               spawnerDefinition.delay = XMLUtils.parseDuration(delay, Duration.ofSeconds(10));
               spawnerDefinition.minDelay = XMLUtils.parseDuration(minDelay, spawnerDefinition.delay);
               spawnerDefinition.maxDelay = XMLUtils.parseDuration(maxDelay, spawnerDefinition.delay);

               if (spawnerDefinition.maxDelay.compareTo(spawnerDefinition.minDelay) < 0){
                    throw new InvalidXMLException("Max delay cannot be smaller than min delay", element);
               }

               spawnerDefinition.maxEntities = XMLUtils.parseNumber(element.getAttribute("max-entities"), Integer.class, Integer.MAX_VALUE);
               spawnerDefinition.playerRange = XMLUtils.parseNumber(element.getAttribute("max-player-range"), Integer.class, Integer.MAX_VALUE);
               // TODO Parse filters

                List<SpawnerObject> objects = new ArrayList<>();
                for (Element object : XMLUtils.getChildren(element, "entity", "item", "tnt", "potion")) {
                   switch (object.getName()) {
                       case "entity":
                           SpawnerObjectEntity entity = new SpawnerObjectEntity(XMLUtils.parseEntityType(object));
                           objects.add(entity);
                           break;
                       case "tnt":

                           break;
                   }
                }
                spawnerDefinition.objects = objects;
            }

            return spawnerModule.spawnerDefinitions.isEmpty() ? null : spawnerModule;
        }



    }
}
