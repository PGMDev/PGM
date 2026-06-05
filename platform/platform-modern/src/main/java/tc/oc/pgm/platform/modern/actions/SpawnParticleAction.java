package tc.oc.pgm.platform.modern.actions;

import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.action.actions.AbstractAction;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.platform.modern.particle.ParticleDefinition;
import tc.oc.pgm.platform.modern.particle.ParticleMatchModule;
import tc.oc.pgm.platform.modern.particle.shapes.ParticleShape;
import tc.oc.pgm.platform.modern.particle.shapes.TextShape;
import tc.oc.pgm.util.math.Formula;

public class SpawnParticleAction<T extends Filterable<?>> extends AbstractAction<T> {

  private final FeatureReference<ParticleDefinition> particle;
  private final Formula<T> xFormula, yFormula, zFormula;

  public SpawnParticleAction(
      Class<T> scope,
      FeatureReference<ParticleDefinition> particle,
      Formula<T> xFormula,
      Formula<T> yFormula,
      Formula<T> zFormula) {
    super(scope);
    this.particle = particle;
    this.xFormula = xFormula;
    this.yFormula = yFormula;
    this.zFormula = zFormula;
  }

  @Override
  public void trigger(T t) {
    ParticleDefinition def = particle.get();
    double x = xFormula.applyAsDouble(t);
    double y = yFormula.applyAsDouble(t);
    double z = zFormula.applyAsDouble(t);
    Location location = new Location(t.getMatch().getWorld(), x, y, z);

    Color overrideColor = null;
    if (def.isTeamColor() && t instanceof MatchPlayer player) {
      TextColor teamTextColor = player.getParty().getTextColor();
      overrideColor = Color.fromRGB(teamTextColor.value());
    }

    ParticleShape shape = def.getShape();
    if (shape instanceof TextShape textShape) {
      String resolved = textShape.resolve(t);
      shape = new TextShape(resolved, null);
    }

    if (shape != null) {
      List<Vector> nodes = shape.getNodes(def.getAmount());
      for (Vector node : nodes) {
        Location nodeLoc = new Location(
            location.getWorld(),
            location.getX() + node.getX(),
            location.getY() + node.getY(),
            location.getZ() + node.getZ());
        ParticleMatchModule.spawnParticle(def, nodeLoc, overrideColor, t.getMatch());
      }
    } else {
      ParticleMatchModule.spawnParticle(def, location, overrideColor, t.getMatch());
    }
  }
}
