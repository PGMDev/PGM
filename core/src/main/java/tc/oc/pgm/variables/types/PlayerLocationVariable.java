package tc.oc.pgm.variables.types;

import static java.lang.Math.toRadians;
import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import org.bukkit.Location;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.variables.VariableDefinition;

public class PlayerLocationVariable extends AbstractVariable<MatchPlayer> {

  private final Component component;
  private Location lastLocation;
  private RayBlockIntersection lastRayCast;

  public PlayerLocationVariable(VariableDefinition<MatchPlayer> definition, Component component) {
    super(definition);
    this.component = component;
  }

  private RayBlockIntersection intersection(MatchPlayer player) {
    if (player.getLocation().equals(lastLocation)) {
      return lastRayCast;
    }
    lastLocation = player.getLocation().clone();
    lastRayCast = PLAYER_UTILS.getTargetedBlock(player.getBukkit());
    return lastRayCast;
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
      case TARGET_X -> intersection(player).getBlock().getX();
      case TARGET_Y -> intersection(player).getBlock().getY();
      case TARGET_Z -> intersection(player).getBlock().getZ();
      case PLACE_X -> intersection(player).getPlaceAt().getX();
      case PLACE_Y -> intersection(player).getPlaceAt().getY();
      case PLACE_Z -> intersection(player).getPlaceAt().getZ();
      case HAS_TARGET -> intersection(player) == null ? 0 : 1;
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
    TARGET_X,
    TARGET_Y,
    TARGET_Z,
    PLACE_X,
    PLACE_Y,
    PLACE_Z,
    HAS_TARGET,
  }
}
