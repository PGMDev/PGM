package tc.oc.pgm.modes;

import static tc.oc.pgm.api.map.ProtoVersions.MODES_IMPLEMENTATION_VERSION;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.material.MaterialData;
import org.jdom2.Document;
import org.jdom2.Element;
import org.joda.time.Duration;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.util.components.PeriodFormats;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class ObjectiveModesModule implements MapModule {

  private List<Mode> modes;
  public static final Duration DEFAULT_SHOW_BEFORE = Duration.standardSeconds(60l);

  private ObjectiveModesModule(List<Mode> modes) {
    this.modes = modes;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new ObjectiveModesMatchModule(match, this.modes);
  }

  public static class Factory implements MapModuleFactory<ObjectiveModesModule> {
    @Override
    public ObjectiveModesModule parse(MapContext context, Logger logger, Document doc)
        throws InvalidXMLException {
      if (context.getInfo().getProto().isOlderThan(MODES_IMPLEMENTATION_VERSION)) {
        return null;
      }

      List<Mode> parsedModes = new ArrayList<>();

      if (doc.getRootElement().getChild("modes") == null) {
        return null;
      }

      for (Element modeEl : XMLUtils.flattenElements(doc.getRootElement(), "modes", "mode")) {
        if (modeEl.getAttributeValue("after") == null) {
          throw new InvalidXMLException("No period has been specified", modeEl);
        }

        MaterialData material =
            XMLUtils.parseBlockMaterialData(Node.fromRequiredAttr(modeEl, "material"));
        long seconds =
            PeriodFormats.SHORTHAND
                .parsePeriod(modeEl.getAttributeValue("after"))
                .toStandardSeconds()
                .getSeconds();
        Duration after = new Duration(seconds * 1000 /*millis*/);
        String name = modeEl.getAttributeValue("name");
        if (name != null) {
          name = ChatColor.translateAlternateColorCodes('`', name);
        }

        String showBeforeRaw = modeEl.getAttributeValue("show-before");
        Duration showBefore =
            showBeforeRaw != null
                ? PeriodFormats.SHORTHAND.parsePeriod(showBeforeRaw).toStandardDuration()
                : DEFAULT_SHOW_BEFORE;

        // Legacy
        boolean legacyShowBossBar = XMLUtils.parseBoolean(modeEl.getAttribute("boss-bar"), true);
        if (!legacyShowBossBar) {
          showBefore = Duration.ZERO;
        }

        for (Mode mode : parsedModes) {
          if (mode.getAfter().equals(after)) {
            throw new InvalidXMLException(
                "Already scheduled a mode for " + after.toStandardSeconds().getSeconds() + "s",
                modeEl);
          }
        }
        parsedModes.add(new Mode(material, after, name, showBefore));
      }

      return new ObjectiveModesModule(parsedModes);
    }
  }
}
