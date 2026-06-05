package tc.oc.pgm.platform.modern.actions;

import org.jdom2.Element;
import tc.oc.pgm.action.ActionParser;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.platform.modern.particle.ParticleDefinition;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class ModernActionParser extends ActionParser {

  public ModernActionParser(MapFactory factory) {
    super(factory);
  }

  @MethodParser("spawn-particle")
  public SpawnParticleAction parseSpawnParticle(Element el, Class<?> scope)
      throws InvalidXMLException {
    var particle =
        getParser().reference(ParticleDefinition.class, el, "particle").required();
    var xFormula = getParser().formula(MatchPlayer.class, el, "x").required();
    var yFormula = getParser().formula(MatchPlayer.class, el, "y").required();
    var zFormula = getParser().formula(MatchPlayer.class, el, "z").required();

    return new SpawnParticleAction(scope, particle, xFormula, yFormula, zFormula);
  }
}
