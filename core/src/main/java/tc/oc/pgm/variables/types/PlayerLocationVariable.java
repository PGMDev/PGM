package tc.oc.pgm.variables.types;

import static java.lang.Math.toRadians;

import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.variables.VariableDefinition;

public class PlayerLocationVariable extends AbstractVariable<MatchPlayer> {

  private final Component component;

  public PlayerLocationVariable(VariableDefinition<MatchPlayer> definition, Component component) {
    super(definition);
    this.component = component;
  }

  @Override
  protected double getValueImpl(MatchPlayer player) {
    return switch (component) {
      case X -> player.getLocation().getX();
      case Y -> player.getLocation().getY();
      case Z -> player.getLocation().getZ();
      case PITCH -> player.getLocation().getPitch();
      case YAW -> player.getLocation().getYaw();
      case DIR_X -> -Math.cos(toRadians(player.getLocation().getPitch()))
          * Math.sin(toRadians(player.getLocation().getYaw()));
      case DIR_Y -> -Math.sin(toRadians(player.getLocation().getPitch()));
      case DIR_Z -> Math.cos(toRadians(player.getLocation().getPitch()))
          * Math.cos(toRadians(player.getLocation().getYaw()));
      case VEL_X -> player.getBukkit().getVelocity().getX();
      case VEL_Y -> player.getBukkit().getVelocity().getY();
      case VEL_Z -> player.getBukkit().getVelocity().getZ();
    };
  }

  @Override
  protected void setValueImpl(MatchPlayer obj, double value) {
    throw new UnsupportedOperationException();
  }

  public enum Component {
    X,
    Y,
    Z,
    PITCH,
    YAW,
    DIR_X,
    DIR_Y,
    DIR_Z,
    VEL_X,
    VEL_Y,
    VEL_Z,
  }
}
