package tc.oc.pgm.variables.types;

import static java.lang.Math.toRadians;
import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import org.bukkit.Location;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.block.RayBlockIntersection;

public class PlayerLocationVariable extends AbstractVariable<MatchPlayer> {

  public static final Map<Component, PlayerLocationVariable> INSTANCES;

  static {
    var values = new EnumMap<Component, PlayerLocationVariable>(Component.class);
    for (Component component : Component.values()) {
      values.put(component, new PlayerLocationVariable(component));
    }
    INSTANCES = Collections.unmodifiableMap(values);
  }

  private static final double NULL_VALUE = -1;
  private static RayCastCache lastRaytrace;

  record RayCastCache(Location location, RayBlockIntersection rayCast) {}

  private final Component component;

  public PlayerLocationVariable(Component component) {
    super(MatchPlayer.class);
    this.component = component;
  }

  @Override
  public boolean isReadonly() {
    return true;
  }

  @Override
  protected double getValueImpl(MatchPlayer player) {
    return component.getter.applyAsDouble(player);
  }

  @Override
  protected void setValueImpl(MatchPlayer obj, double value) {
    throw new UnsupportedOperationException();
  }

  public enum Component {
    X(p -> p.getLocation().getX()),
    Y(p -> p.getLocation().getY()),
    Z(p -> p.getLocation().getZ()),
    PITCH(p -> p.getLocation().getPitch()),
    YAW(p -> p.getLocation().getYaw()),
    DIR_X(p -> -Math.cos(toRadians(p.getLocation().getPitch()))
        * Math.sin(toRadians(p.getLocation().getYaw()))),
    DIR_Y(p -> -Math.sin(toRadians(p.getLocation().getPitch()))),
    DIR_Z(p -> Math.cos(toRadians(p.getLocation().getPitch()))
        * Math.cos(toRadians(p.getLocation().getYaw()))),
    VEL_X(p -> p.getBukkit().getVelocity().getX()),
    VEL_Y(p -> p.getBukkit().getVelocity().getY()),
    VEL_Z(p -> p.getBukkit().getVelocity().getZ()),
    TARGET_X(p -> intersection(p, i -> i.getBlock().getX())),
    TARGET_Y(p -> intersection(p, i -> i.getBlock().getY())),
    TARGET_Z(p -> intersection(p, i -> i.getBlock().getZ())),
    PLACE_X(p -> intersection(p, i -> i.getPlaceAt().getX())),
    PLACE_Y(p -> intersection(p, i -> i.getPlaceAt().getY())),
    PLACE_Z(p -> intersection(p, i -> i.getPlaceAt().getZ())),
    HAS_TARGET(p -> intersection(p) == null ? 0 : 1);

    private final ToDoubleFunction<MatchPlayer> getter;

    Component(ToDoubleFunction<MatchPlayer> getter) {
      this.getter = getter;
    }
  }

  private static RayBlockIntersection intersection(MatchPlayer player) {
    RayCastCache cache = lastRaytrace;
    if (cache != null && player.getLocation().equals(cache.location)) {
      return cache.rayCast;
    }
    lastRaytrace = cache = new RayCastCache(
        player.getLocation().clone(), PLAYER_UTILS.getTargetedBlock(player.getBukkit()));
    return cache.rayCast;
  }

  private static double intersection(
      MatchPlayer player, ToDoubleFunction<RayBlockIntersection> toDouble) {
    var intersection = intersection(player);
    return intersection == null ? NULL_VALUE : toDouble.applyAsDouble(intersection);
  }
}
