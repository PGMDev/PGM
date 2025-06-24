package tc.oc.pgm.variables.types;

import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.features.StateHolder;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.regions.Bounds;
import tc.oc.pgm.regions.CuboidRegion;
import tc.oc.pgm.variables.Variable;
import tc.oc.pgm.variables.VariablesMatchModule;

public class CuboidVariable extends AbstractVariable<Match>
    implements Variable.Indexed<Match>, StateHolder<CuboidRegion.Mutable>, RegionDefinition {
  private static final Component[] COMPONENTS = Component.values();
  private final CuboidRegion cuboid;

  private Cached lastLookup = null;

  public CuboidVariable(CuboidRegion cuboid) {
    super(Match.class);
    this.cuboid = cuboid;
  }

  @Override
  public void load(Match match) {
    match.getFeatureContext().registerState(this, cuboid.asMutableCopy());
  }

  @Override
  public boolean isDynamic() {
    return false;
  }

  record Cached(CuboidRegion.Mutable cuboid, String world) {}

  private CuboidRegion.Mutable getCuboid(World world) {
    var cached = lastLookup;
    if (cached == null || !cached.world.equals(world.getName())) {
      var match = PGM.get().getMatchManager().getMatch(world);
      if (match == null) throw new IllegalStateException("Couldn't find match");
      cached = lastLookup = new Cached(match.state(this), world.getName());
    }
    return cached.cuboid;
  }

  public int checkBounds(int idx, Filterable<?> obj) {
    if (idx < 0 || idx >= COMPONENTS.length) {
      String id = obj.moduleRequire(VariablesMatchModule.class).getId(this);
      PGM.get()
          .getGameLogger()
          .log(Level.SEVERE, String.format("Index %d out of bounds for cuboid %s", idx, id));
      return 0;
    }
    return idx;
  }

  @Override
  public double getValue(Filterable<?> obj, int idx) {
    return getComponent(obj, COMPONENTS[checkBounds(idx, obj)]);
  }

  @Override
  public void setValue(Filterable<?> obj, int idx, double value) {
    setComponent(obj, COMPONENTS[checkBounds(idx, obj)], value);

    // For performance reasons, let's avoid launching an event for every variable change
    obj.moduleRequire(FilterMatchModule.class).invalidate(obj);
  }

  @Override
  public int size() {
    return COMPONENTS.length;
  }

  @Override
  protected double getValueImpl(Match obj) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void setValueImpl(Match obj, double value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean contains(Location point) {
    return getCuboid(point.getWorld()).contains(point.toVector());
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    return match.state(this);
  }

  @Override
  public Bounds getBounds() {
    return cuboid.getBounds();
  }

  @Override
  public boolean isBlockBounded() {
    return cuboid.isBlockBounded();
  }

  @Override
  public boolean canGetRandom() {
    return cuboid.canGetRandom();
  }

  public enum Component {
    MIN_X,
    MIN_Y,
    MIN_Z,
    MAX_X,
    MAX_Y,
    MAX_Z;
    private final Function<CuboidRegion.Mutable, Vector> vector = name().startsWith("MIN")
        ? CuboidRegion.Mutable::getMutableMin
        : CuboidRegion.Mutable::getMutableMax;
    private final ToDoubleFunction<Vector> getter =
        switch (name().charAt(4)) {
          case 'X' -> Vector::getX;
          case 'Y' -> Vector::getY;
          case 'Z' -> Vector::getZ;
          default -> throw new IllegalStateException("Unexpected value: " + name());
        };
    private final ObjDoubleConsumer<Vector> setter =
        switch (name().charAt(4)) {
          case 'X' -> Vector::setX;
          case 'Y' -> Vector::setY;
          case 'Z' -> Vector::setZ;
          default -> throw new IllegalStateException("Unexpected value: " + name());
        };
  }

  public double getComponent(Filterable<?> match, Component component) {
    return component.getter.applyAsDouble(component.vector.apply(match.state(this)));
  }

  public void setComponent(Filterable<?> match, Component component, double value) {
    component.setter.accept(component.vector.apply(match.state(this)), value);
  }

  public Variable<Match> getComponent(Component component) {
    return new ComponentVariable(component);
  }

  class ComponentVariable extends AbstractVariable<Match> {
    private final Component component;

    public ComponentVariable(Component component) {
      super(Match.class);
      this.component = component;
    }

    @Override
    protected double getValueImpl(Match obj) {
      return getComponent(obj, component);
    }

    @Override
    protected void setValueImpl(Match obj, double value) {
      setComponent(obj, component, value);
    }
  }
}
