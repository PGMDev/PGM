package tc.oc.pgm.compass;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.flag.FlagModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class CompassModule implements MapModule<CompassMatchModule> {

  private final ImmutableList<CompassTarget<?>> compassTargets;
  private final OrderStrategy orderStrategy;
  private final boolean showDistance;

  public CompassModule(
      ImmutableList<CompassTarget<?>> compassTargets,
      OrderStrategy orderStrategy,
      boolean showDistance) {
    this.compassTargets = compassTargets;
    this.orderStrategy = orderStrategy;
    this.showDistance = showDistance;
  }

  @Nullable
  @Override
  public Collection<Class<? extends MatchModule>> getHardDependencies() {
    return ImmutableList.of(FilterMatchModule.class);
  }

  @Override
  public @Nullable CompassMatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new CompassMatchModule(match, compassTargets, orderStrategy, showDistance);
  }

  public static class Factory implements MapModuleFactory<CompassModule> {

    @Override
    public @Nullable Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return List.of(FlagModule.class);
    }

    @Override
    public @Nullable CompassModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      CompassParser parser = new CompassParser(factory);

      ImmutableList.Builder<CompassTarget<?>> compassTargets = ImmutableList.builder();
      OrderStrategy orderStrategy = OrderStrategy.DEFINITION_ORDER;
      boolean showDistance = false;
      for (Element compassRoot : doc.getRootElement().getChildren("compass")) {
        orderStrategy = XMLUtils.parseEnum(
            Node.fromAttr(compassRoot, "order"), OrderStrategy.class, orderStrategy);
        showDistance =
            XMLUtils.parseBoolean(Node.fromAttr(compassRoot, "show-distance"), showDistance);

        List<Element> children = compassRoot.getChildren();
        if (children.isEmpty())
          throw new InvalidXMLException("Compass tag has no targets", compassRoot);
        for (Element compassTarget : children) {
          compassTargets.add(parser.parseCompassTarget(compassTarget));
        }
      }
      ImmutableList<CompassTarget<?>> targets = compassTargets.build();
      if (targets.isEmpty()) return null;
      return new CompassModule(targets, orderStrategy, showDistance);
    }
  }
}
