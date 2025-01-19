package tc.oc.pgm.variables.types;

import static java.lang.Math.toRadians;
import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;
import org.bukkit.Location;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.block.RayBlockIntersection;

public class PlayerVariable extends AbstractVariable<MatchPlayer> {

  private static final Map<Component, PlayerVariable> INSTANCES;

  static {
    var values = new EnumMap<Component, PlayerVariable>(Component.class);
    for (Component component : Component.values()) {
      values.put(component, new PlayerVariable(component.getter, component.setter));
    }
    INSTANCES = Collections.unmodifiableMap(values);
  }

  private static final double NULL_VALUE = -1;
  private static RayCastCache lastRaytrace;

  record RayCastCache(Location location, RayBlockIntersection rayCast) {}

  private final ToDoubleFunction<Player> getter;
  private final ObjDoubleConsumer<Player> setter;

  private PlayerVariable(ToDoubleFunction<Player> getter, ObjDoubleConsumer<Player> setter) {
    super(MatchPlayer.class);
    this.getter = getter;
    this.setter = setter;
  }

  public static PlayerVariable of(Component component) {
    return INSTANCES.get(component);
  }

  @Override
  public boolean isReadonly() {
    return setter == null;
  }

  @Override
  protected double getValueImpl(MatchPlayer player) {
    return getter.applyAsDouble(player.getBukkit());
  }

  @Override
  protected void setValueImpl(MatchPlayer obj, double value) {
    if (setter == null) throw new UnsupportedOperationException();
    setter.accept(obj.getBukkit(), value);
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
    VEL_X(p -> p.getVelocity().getX()),
    VEL_Y(p -> p.getVelocity().getY()),
    VEL_Z(p -> p.getVelocity().getZ()),
    TARGET_X(p -> intersection(p, i -> i.getBlock().getX())),
    TARGET_Y(p -> intersection(p, i -> i.getBlock().getY())),
    TARGET_Z(p -> intersection(p, i -> i.getBlock().getZ())),
    PLACE_X(p -> intersection(p, i -> i.getPlaceAt().getX())),
    PLACE_Y(p -> intersection(p, i -> i.getPlaceAt().getY())),
    PLACE_Z(p -> intersection(p, i -> i.getPlaceAt().getZ())),
    HAS_TARGET(p -> intersection(p) == null ? 0 : 1),
    HEALTH(
        Damageable::getHealth, (p, h) -> p.setHealth(Math.max(0, Math.min(p.getMaxHealth(), h)))),
    MAX_HEALTH(Damageable::getMaxHealth, (p, h) -> p.setMaxHealth(Math.max(0.1f, h))),
    FOOD(Player::getFoodLevel, (p, f) -> p.setFoodLevel((int) f)),
    SATURATION(Player::getSaturation, (p, s) -> p.setSaturation((float) s)),
    EXPERIENCE(Player::getTotalExperience, (p, ex) -> p.setTotalExperience((int) ex)),
    EXP_PROGRESS(Player::getExp, (p, ex) -> p.setExp((float) ex)),
    LEVEL(Player::getLevel, (p, l) -> p.setLevel((int) l));

    private final ToDoubleFunction<Player> getter;
    private final ObjDoubleConsumer<Player> setter;

    Component(ToDoubleFunction<Player> getter, ObjDoubleConsumer<Player> setter) {
      this.getter = getter;
      this.setter = setter;
    }

    Component(ToDoubleFunction<Player> getter) {
      this(getter, null);
    }
  }

  private static RayBlockIntersection intersection(Player player) {
    RayCastCache cache = lastRaytrace;
    var loc = player.getLocation();
    if (cache != null && loc.equals(cache.location)) return cache.rayCast;
    lastRaytrace = cache = new RayCastCache(loc, PLAYER_UTILS.getTargetedBlock(player));
    return cache.rayCast;
  }

  private static double intersection(
      Player player, ToDoubleFunction<RayBlockIntersection> toDouble) {
    var intersection = intersection(player);
    return intersection == null ? NULL_VALUE : toDouble.applyAsDouble(intersection);
  }
}
