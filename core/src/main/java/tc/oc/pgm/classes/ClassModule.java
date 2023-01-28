package tc.oc.pgm.classes;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.material.MaterialData;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ClassModule implements MapModule<ClassMatchModule> {

  private static final Collection<MapTag> TAGS =
      ImmutableList.of(new MapTag("classes", "Classes", false, true));
  final String family;
  final Map<String, PlayerClass> classes;
  final PlayerClass defaultClass;

  public ClassModule(String family, Map<String, PlayerClass> classes, PlayerClass defaultClass) {
    this.family = assertNotNull(family, "family");
    this.classes = ImmutableMap.copyOf(assertNotNull(classes, "classes"));
    this.defaultClass = assertNotNull(defaultClass, "default class");
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  @Override
  public ClassMatchModule createMatchModule(Match match) {
    return new ClassMatchModule(match, this.family, this.classes, this.defaultClass);
  }

  public String getFamily() {
    return this.family;
  }

  public @Nullable PlayerClass getPlayerClass(String name) {
    return this.classes.get(name);
  }

  public Set<PlayerClass> getPlayerClasses() {
    return ImmutableSet.copyOf(this.classes.values());
  }

  public static class Factory implements MapModuleFactory<ClassModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return Collections.singleton(KitModule.class);
    }

    @Override
    public ClassModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<Element> classElements =
          XMLUtils.flattenElements(doc.getRootElement(), "classes", "class");
      if (classElements.isEmpty()) return null;

      // first look for the family
      Map<String, Integer> familyFrequency = Maps.newHashMap();
      String family = null;
      int familyOccurances = 0;

      for (Element classEl : classElements) {
        String classFamily = classEl.getAttributeValue("family");
        if (classFamily != null) {
          Integer num = familyFrequency.get(classFamily);
          familyFrequency.put(classFamily, (num != null ? num.intValue() : 0) + 1);
        }
      }

      for (Map.Entry<String, Integer> entry : familyFrequency.entrySet()) {
        if (entry.getValue() > familyOccurances) {
          family = entry.getKey();
        } else if (entry.getValue() == familyOccurances) {
          family = null;
        }
      }

      if (family == null)
        throw new InvalidXMLException("Unable to determine family for classes", doc);

      Set<String> usedNames = Sets.newHashSet();
      ImmutableMap.Builder<String, PlayerClass> builder = ImmutableMap.builder();
      PlayerClass defaultClass = null;

      for (Element classEl : classElements) {
        PlayerClass cls = parseClass(classEl, factory.getKits(), family);

        if (usedNames.contains(cls.getName().toLowerCase())) {
          throw new InvalidXMLException(
              "Class already registered to \" + cls.getName() + \"; skipping second instance",
              classEl);
        }

        String classFamily = classEl.getAttributeValue("family");
        if (family == null) {
          family = classFamily;
        } else {
          if (!family.equals(classFamily)) {
            throw new InvalidXMLException(
                "Family was determined to be '" + family + "' but class specified '" + classFamily,
                classEl);
          }
        }

        if (XMLUtils.parseBoolean(classEl.getAttribute("default"), false)) {
          if (defaultClass != null) {
            throw new InvalidXMLException(
                "Default class already registered to " + defaultClass.getName(), classEl);
          } else {
            defaultClass = cls;
          }
        }

        builder.put(cls.getName(), cls);
      }

      Map<String, PlayerClass> classes = builder.build();

      if (!classes.isEmpty()) {
        if (defaultClass != null) {
          return new ClassModule(family, classes, defaultClass);
        } else {
          throw new InvalidXMLException("No default class set", doc);
        }
      }

      return null;
    }

    private static PlayerClass parseClass(Element classEl, KitParser kitParser, String family)
        throws InvalidXMLException {
      String name = classEl.getAttributeValue("name");
      if (name == null) {
        throw new InvalidXMLException("class must have a name", classEl);
      }

      String description = classEl.getAttributeValue("description");
      if (description != null) {
        description = BukkitUtils.colorize(description);
      }

      String longdescription = classEl.getAttributeValue("longdescription");
      if (longdescription != null) {
        longdescription = BukkitUtils.colorize(longdescription);
      }

      boolean sticky = XMLUtils.parseBoolean(classEl.getAttribute("sticky"), false);

      ImmutableSet.Builder<Kit> kits = ImmutableSet.builder();
      for (Element kitEl : classEl.getChildren("kit")) {
        Kit kit = kitParser.parse(kitEl);
        kits.add(kit);
      }

      MaterialData icon = XMLUtils.parseMaterialData(Node.fromRequiredAttr(classEl, "icon"));

      boolean restrict = XMLUtils.parseBoolean(classEl.getAttribute("restrict"), false);

      return new PlayerClass(
          name, family, description, longdescription, sticky, kits.build(), icon, restrict);
    }
  }
}
