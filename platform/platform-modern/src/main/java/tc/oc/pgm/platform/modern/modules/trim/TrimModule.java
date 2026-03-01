package tc.oc.pgm.platform.modern.modules.trim;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.util.inventory.Slot;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.xml.InvalidXMLException;

@NullMarked
public class TrimModule implements MapModule<TrimMatchModule> {
  private final TrimProperties properties;

  public TrimModule(TrimProperties properties) {
    this.properties = properties;
  }

  @Override
  public TrimMatchModule createMatchModule(Match match) {
    return new TrimMatchModule(match, this.properties);
  }

  public static class Factory implements MapModuleFactory<TrimModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return List.of(RegionModule.class);
    }

    @Override
    public @Nullable TrimModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      var parser = factory.getParser();
      boolean enabled = true;
      Map<EquipmentSlot, TrimPattern> patterns = new HashMap<>();
      patterns.put(EquipmentSlot.HEAD, TrimPattern.HOST);
      patterns.put(EquipmentSlot.CHEST, TrimPattern.RAISER);
      patterns.put(EquipmentSlot.LEGS, TrimPattern.DUNE);
      patterns.put(EquipmentSlot.FEET, TrimPattern.DUNE);

      for (Element el : doc.getRootElement().getChildren("trims")) {
        enabled = parser.parseBool(el, "enabled").optional(enabled);

        for (Slot.Armor slot : Slot.Armor.armor().toList()) {
          var curr = patterns.get(slot.toEquipmentSlot());
          var trim = parser.primitive(this::parseTrim, el, slot.armorTypeName()).optional(curr);
          if (curr == trim) continue;

          if (trim == null) patterns.remove(slot.toEquipmentSlot());
          else patterns.put(slot.toEquipmentSlot(), trim);
        }
      }

      return enabled && !patterns.isEmpty()
          ? new TrimModule(new TrimProperties(Map.copyOf(patterns)))
          : null;
    }

    private @Nullable TrimPattern parseTrim(String key) {
      if ("none".equalsIgnoreCase(key)) return null;
      var trim = Trims.getPatternByKey(key);
      if (trim == null) throw TextException.invalidFormat(key, TrimPattern.class);
      return trim;
    }
  }
}
