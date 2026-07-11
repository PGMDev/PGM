package tc.oc.pgm.platform.modern.action.actions;

import org.jdom2.Element;
import tc.oc.pgm.action.ActionParser;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.platform.modern.action.SpawnMannequinAction;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinDefinition;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class ModernActionParser extends ActionParser {

  public ModernActionParser(MapFactory factory) {
    super(factory);
  }

  @MethodParser("spawn-mannequin")
  public <B extends Filterable<?>> SpawnMannequinAction<B> parseSpawnMannequin(
      Element el, Class<B> scope) throws InvalidXMLException {
    scope = parseScope(el, scope);
    var mannequin =
        getParser().reference(MannequinDefinition.class, el, "mannequin").required();
    var xformula = getParser().formula(scope, el, "x").required();
    var yformula = getParser().formula(scope, el, "y").required();
    var zformula = getParser().formula(scope, el, "z").required();

    return new SpawnMannequinAction<>(scope, mannequin, xformula, yformula, zformula);
  }
}
