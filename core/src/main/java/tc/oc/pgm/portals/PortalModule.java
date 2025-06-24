package tc.oc.pgm.portals;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
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
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.operator.InverseFilter;
import tc.oc.pgm.filters.parse.DynamicFilterValidation;
import tc.oc.pgm.regions.RFAContext;
import tc.oc.pgm.regions.RFAScope;
import tc.oc.pgm.regions.RegionFilterApplication;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.TranslatedRegion;
import tc.oc.pgm.regions.Union;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class PortalModule implements MapModule<PortalMatchModule> {
  private static final Component PROTECT_MESSAGE = translatable("map.protectPortal");

  protected final Set<Portal> portals;

  public PortalModule(Set<Portal> portals) {
    this.portals = ImmutableSet.copyOf(portals);
  }

  @Nullable
  @Override
  public Collection<Class<? extends MatchModule>> getHardDependencies() {
    return ImmutableList.of(FilterMatchModule.class);
  }

  @Override
  public PortalMatchModule createMatchModule(Match match) {
    return new PortalMatchModule(match, this.portals);
  }

  public static class Factory implements MapModuleFactory<PortalModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(RegionModule.class, FilterModule.class);
    }

    @Override
    public PortalModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      var parser = factory.getParser();
      Set<Portal> portals = Sets.newHashSet();
      RFAContext.Builder rfaContext = factory.getModule(RegionModule.class).getRFAContextBuilder();

      for (Element portalEl : XMLUtils.flattenElements(doc.getRootElement(), "portals", "portal")) {

        PortalTransform transform = PortalTransform.piecewise(
            parseDoubleProvider(portalEl, "x", RelativeDoubleProvider.ZERO),
            parseDoubleProvider(portalEl, "y", RelativeDoubleProvider.ZERO),
            parseDoubleProvider(portalEl, "z", RelativeDoubleProvider.ZERO),
            parseDoubleProvider(portalEl, "yaw", RelativeDoubleProvider.ZERO),
            parseDoubleProvider(portalEl, "pitch", RelativeDoubleProvider.ZERO));

        Region.Static entrance;
        if (factory.getProto().isOlderThan(MapProtos.MODULE_SUBELEMENT_VERSION)) {
          entrance = parser.staticRegion(portalEl).children().orNull();
        } else {
          entrance = parser.staticRegion(portalEl, "region").orNull();
        }

        Region.Static exit =
            parser.staticRegion(portalEl, "destination").randomPoints().orNull();

        if (exit != null) {
          // If there is an explicit exit region, create a transform for it and combine
          // it with the piecewise transform (so angle transforms are still applied).
          transform = PortalTransform.concatenate(
              transform, PortalTransform.regional(Optional.ofNullable(entrance), exit));
        } else if (entrance != null && transform.invertible()) {
          // If no exit region is specified, but there is an entrance region, and the
          // piecewise transform is invertible, infer the exit region from the entrance region.
          exit = new PortalExitRegion(entrance, transform);
        }

        // Dynamic filters
        Filter forward =
            factory.getFilters().parseProperty(portalEl, "forward", DynamicFilterValidation.PLAYER);
        Filter reverse =
            factory.getFilters().parseProperty(portalEl, "reverse", DynamicFilterValidation.PLAYER);
        Filter transit =
            factory.getFilters().parseProperty(portalEl, "transit", DynamicFilterValidation.PLAYER);

        // Check for conflicting dynamic filters
        if (transit != null && (forward != null || reverse != null)) {
          throw new InvalidXMLException(
              "Cannot combine 'transit' property with 'forward' or 'transit' properties", portalEl);
        }

        // Check for conflicting region and dynamic filter at each end of the portal
        if (entrance != null && (forward != null || transit != null)) {
          throw new InvalidXMLException(
              "Cannot combine an entrance region with 'forward' or 'transit' properties", portalEl);
        }

        if (exit != null && (reverse != null || transit != null)) {
          throw new InvalidXMLException(
              "Cannot combine an exit region with 'reverse' or 'transit' properties", portalEl);
        }

        // Figure out the forward trigger, from the dynamic filters or entrance region
        Filter forwardFinal = Stream.of(forward, transit, entrance)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> new InvalidXMLException(
                "Portal must have an entrance region, or one of 'forward' or 'transit' properties",
                portalEl));

        // Figure out the (optional) reverse trigger, from dynamic filters or exit region
        Filter inverseTransit = transit != null ? new InverseFilter(transit) : null;

        final Optional<Filter> reverseFinal =
            Stream.of(reverse, inverseTransit, exit).filter(Objects::nonNull).findFirst();

        // Portal is always bidirectional if a reverse dynamic filter is specified,
        // otherwise it must be enabled explicitly.
        final boolean bidirectional = reverse != null
            || transit != null
            || XMLUtils.parseBoolean(portalEl.getAttribute("bidirectional"), false);
        if (bidirectional && !transform.invertible()) {
          throw new InvalidXMLException(
              "Bidirectional portal must have an invertible transform", portalEl);
        }

        // Passive filters
        Filter participantFilter =
            factory.getFilters().parseFilterProperty(portalEl, "filter", StaticFilter.ALLOW);

        Filter observerFilter =
            factory.getFilters().parseFilterProperty(portalEl, "observers", StaticFilter.ALLOW);

        boolean sound = XMLUtils.parseBoolean(portalEl.getAttribute("sound"), true);
        boolean smooth = XMLUtils.parseBoolean(portalEl.getAttribute("smooth"), false);

        boolean protect = XMLUtils.parseBoolean(portalEl.getAttribute("protect"), false);

        // Protect the entrance/exit
        if (protect) {
          protectRegion(rfaContext, entrance);
          if (exit != null) {
            protectRegion(rfaContext, exit);
          }
        }

        Portal portal =
            new Portal(forwardFinal, transform, participantFilter, observerFilter, sound, smooth);
        portals.add(portal);
        factory.getFeatures().addFeature(portalEl, portal);

        if (bidirectional) {
          Portal inversePortal = new Portal(
              reverseFinal.orElse(null),
              transform.inverse(),
              participantFilter,
              observerFilter,
              sound,
              smooth);
          portals.add(inversePortal);
          factory.getFeatures().addFeature(portalEl, inversePortal);
        }
      }

      return portals.isEmpty() ? null : new PortalModule(portals);
    }

    /**
     * Use an {@link RegionFilterApplication} to protect the given entrance/exit {@link Region}.
     *
     * <p>The region is extended up by 2m to allow for the height of the player.
     */
    private static void protectRegion(RFAContext.Builder rfaContext, Region region) {
      region = Union.of(
          region,
          TranslatedRegion.translate(region, new Vector(0, 1, 0)),
          TranslatedRegion.translate(region, new Vector(0, 2, 0)));

      rfaContext.prepend(new RegionFilterApplication(
          RFAScope.BLOCK_PLACE, region, StaticFilter.DENY, PROTECT_MESSAGE, false));
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
}
