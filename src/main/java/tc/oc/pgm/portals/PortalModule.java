package tc.oc.pgm.portals;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.map.ProtoVersions;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.regions.*;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Portal",
    depends = {RegionModule.class, FilterModule.class})
public class PortalModule extends MapModule {
  private static final Component PROTECT_MESSAGE =
      new PersonalizedTranslatable("match.portal.protectMessage");

  protected final Set<Portal> portals;

  public PortalModule(Set<Portal> portals) {
    this.portals = ImmutableSet.copyOf(portals);
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new PortalMatchModule(match, this.portals);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static PortalModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    Set<Portal> portals = Sets.newHashSet();
    RegionParser regionParser = context.getRegionParser();
    RFAContext rfaContext = context.getModule(RegionModule.class).getRFAContext();
    Filter protectionFilter = StaticFilter.DENY;

    for (Element portalEl : XMLUtils.flattenElements(doc.getRootElement(), "portals", "portal")) {
      DoubleProvider dx = parseDoubleProvider(portalEl, "x", RelativeDoubleProvider.ZERO);
      DoubleProvider dy = parseDoubleProvider(portalEl, "y", RelativeDoubleProvider.ZERO);
      DoubleProvider dz = parseDoubleProvider(portalEl, "z", RelativeDoubleProvider.ZERO);
      DoubleProvider dYaw = parseDoubleProvider(portalEl, "yaw", RelativeDoubleProvider.ZERO);
      DoubleProvider dPitch = parseDoubleProvider(portalEl, "pitch", RelativeDoubleProvider.ZERO);

      Region region;
      if (context.getProto().isOlderThan(ProtoVersions.MODULE_SUBELEMENT_VERSION)) {
        region = regionParser.parseChildren(portalEl);
      } else {
        region = regionParser.parseRequiredRegionProperty(portalEl, "region");
      }

      Region destinationRegion =
          regionParser.parseRegionProperty(
              portalEl, RandomPointsValidation.INSTANCE, "destination");

      Filter filter =
          context.getFilterParser().parseFilterProperty(portalEl, "filter", StaticFilter.ALLOW);

      boolean sound = XMLUtils.parseBoolean(portalEl.getAttribute("sound"), true);
      boolean protect = XMLUtils.parseBoolean(portalEl.getAttribute("protect"), false);
      boolean bidirectional = XMLUtils.parseBoolean(portalEl.getAttribute("bidirectional"), false);
      boolean smooth = XMLUtils.parseBoolean(portalEl.getAttribute("smooth"), false);

      Portal portal;
      try {
        portal =
            new Portal(
                region,
                dx,
                dy,
                dz,
                dYaw,
                dPitch,
                destinationRegion,
                filter,
                sound,
                protect,
                bidirectional,
                smooth);
      } catch (IllegalArgumentException e) {
        throw new InvalidXMLException(
            e.getMessage(), portalEl); // Probably non-relative bidirectional
      }

      portals.add(portal);
      context.features().addFeature(portalEl, portal);

      if (portal.isProtected()) {
        // Protect the entrance
        RegionFilterApplication rfa =
            new RegionFilterApplication(
                RFAScope.BLOCK_PLACE,
                Union.of(
                    portal.getRegion(),
                    TranslatedRegion.translate(portal.getRegion(), new Vector(0, 1, 0)),
                    TranslatedRegion.translate(portal.getRegion(), new Vector(0, 2, 0))),
                protectionFilter,
                PROTECT_MESSAGE,
                false);

        rfaContext.prepend(rfa);

        // Protect the exit, but only if the destination coords are all relative or all absolute
        if (dx instanceof RelativeDoubleProvider
            && dy instanceof RelativeDoubleProvider
            && dz instanceof RelativeDoubleProvider) {

          rfa =
              new RegionFilterApplication(
                  RFAScope.BLOCK_PLACE,
                  Union.of(
                      TranslatedRegion.translate(
                          portal.getRegion(), new Vector(dx.get(0), dy.get(0), dz.get(0))),
                      TranslatedRegion.translate(
                          portal.getRegion(), new Vector(dx.get(0), dy.get(1), dz.get(0))),
                      TranslatedRegion.translate(
                          portal.getRegion(), new Vector(dx.get(0), dy.get(2), dz.get(0)))),
                  protectionFilter,
                  PROTECT_MESSAGE,
                  false);

          rfaContext.prepend(rfa);

        } else if (dx instanceof StaticDoubleProvider
            && dy instanceof StaticDoubleProvider
            && dz instanceof StaticDoubleProvider) {
          /**
           * Protect a region roughly the size of the player at the portal exit, expanded by half a
           * block in all directions. If the destination is the center of a block surface, only the
           * two blocks above that point will be protected.
           */
          Vector destination = new Vector(dx.get(0), dy.get(0), dz.get(0));
          rfa =
              new RegionFilterApplication(
                  RFAScope.BLOCK_PLACE,
                  new CuboidRegion(
                      destination.clone().subtract(new Vector(0.9, 0.5, 0.9)),
                      destination.clone().add(new Vector(0.9, 2.4, 0.9))),
                  protectionFilter,
                  PROTECT_MESSAGE,
                  false);

          rfaContext.prepend(rfa);
        } else if (destinationRegion != null) {
          rfa =
              new RegionFilterApplication(
                  RFAScope.BLOCK_PLACE,
                  destinationRegion,
                  protectionFilter,
                  PROTECT_MESSAGE,
                  false);

          rfaContext.prepend(rfa);
        }
      }
    }

    if (portals.size() == 0) {
      return null;
    } else {
      return new PortalModule(portals);
    }
  }

  private static DoubleProvider parseDoubleProvider(
      Element el, String attributeName, DoubleProvider def) throws InvalidXMLException {
    Attribute attr = el.getAttribute(attributeName);
    if (attr == null) {
      return def;
    }
    String text = attr.getValue();
    try {
      if (text.startsWith("@")) {
        double value = Double.parseDouble(text.substring(1));
        return new StaticDoubleProvider(value);
      } else {
        double value = Double.parseDouble(text);
        return new RelativeDoubleProvider(value);
      }
    } catch (NumberFormatException e) {
      throw new InvalidXMLException("Invalid portal coordinate", attr, e);
    }
  }
}
