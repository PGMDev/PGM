package tc.oc.pgm.consumable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.action.ActionModule;
import tc.oc.pgm.action.ActionParser;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ConsumableModule implements MapModule<ConsumableMatchModule> {

  private final ImmutableMap<String, ConsumableDefinition> consumables;

  private ConsumableModule(ImmutableMap<String, ConsumableDefinition> consumables) {
    this.consumables = consumables;
  }

  @Override
  public @Nullable ConsumableMatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new ConsumableMatchModule(match, consumables);
  }

  public static class Factory implements MapModuleFactory<ConsumableModule> {

    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(ActionModule.class);
    }

    @Override
    public @Nullable ConsumableModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      ActionParser actionParser = new ActionParser(factory);

      var builder = ImmutableMap.<String, ConsumableDefinition>builder();

      for (Element el :
          XMLUtils.flattenElements(doc.getRootElement(), "consumables", "consumable")) {
        String id = XMLUtils.getRequiredAttribute(el, "id").getValue();

        Node actionNode = Node.fromRequiredAttr(el, "action", "kit");
        Action<? super MatchPlayer> action =
            actionParser.parseReference(actionNode, MatchPlayer.class);

        ConsumeCause cause =
            XMLUtils.parseEnum(Node.fromRequiredAttr(el, "on"), ConsumeCause.class);

        boolean override = XMLUtils.parseBoolean(el.getAttribute("override"), true);
        boolean consume =
            XMLUtils.parseBoolean(el.getAttribute("consume"), cause == ConsumeCause.EAT);

        if (!consume && !override && cause == ConsumeCause.EAT) {
          throw new InvalidXMLException(
              "Not consuming while not overriding isn't supported for EAT", el);
        }

        var consumable = new ConsumableDefinition(id, action, cause, override, consume);

        factory.getFeatures().addFeature(el, consumable);
        builder.put(id, consumable);
      }
      var built = builder.build();

      return built.isEmpty() ? null : new ConsumableModule(built);
    }
  }
}
