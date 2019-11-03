package tc.oc.pgm.modules;

import java.util.logging.Logger;
import org.bukkit.event.Listener;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.map.ProtoVersions;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Playable Region",
    depends = {RegionModule.class})
public class PlayableRegionModule extends MapModule implements Listener {
  protected final Region playableRegion;

  public PlayableRegionModule(Region playableRegion) {
    this.playableRegion = playableRegion;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new PlayableRegionMatchModule(match, this.playableRegion);
  }

  public static PlayableRegionModule parse(MapModuleContext context, Logger log, Document doc)
      throws InvalidXMLException {
    Element playableRegionElement = doc.getRootElement().getChild("playable");
    if (playableRegionElement != null) {
      if (context.getProto().isOlderThan(ProtoVersions.MODULE_SUBELEMENT_VERSION)) {
        return new PlayableRegionModule(
            context.getRegionParser().parseChildren(playableRegionElement));
      } else {
        throw new InvalidXMLException(
            "Module is discontinued as of " + ProtoVersions.MODULE_SUBELEMENT_VERSION.toString(),
            playableRegionElement);
      }
    }

    return null;
  }
}
