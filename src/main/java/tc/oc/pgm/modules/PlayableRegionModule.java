package tc.oc.pgm.modules;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.bukkit.event.Listener;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.ProtoVersions;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.xml.InvalidXMLException;

public class PlayableRegionModule implements MapModule, Listener {
  protected final Region playableRegion;

  public PlayableRegionModule(Region playableRegion) {
    this.playableRegion = playableRegion;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new PlayableRegionMatchModule(match, this.playableRegion);
  }

  public static class Factory implements MapModuleFactory<PlayableRegionModule> {
    @Override
    public Collection<Class<? extends MapModule>> getSoftDependencies() {
      return ImmutableList.of(RegionModule.class);
    }

    @Override
    public PlayableRegionModule parse(MapContext context, Logger logger, Document doc)
        throws InvalidXMLException {
      Element playableRegionElement = doc.getRootElement().getChild("playable");
      if (playableRegionElement != null) {
        if (context.getInfo().getProto().isOlderThan(ProtoVersions.MODULE_SUBELEMENT_VERSION)) {
          return new PlayableRegionModule(
              context.legacy().getRegions().parseChildren(playableRegionElement));
        } else {
          throw new InvalidXMLException(
              "Module is discontinued as of " + ProtoVersions.MODULE_SUBELEMENT_VERSION.toString(),
              playableRegionElement);
        }
      }

      return null;
    }
  }
}
