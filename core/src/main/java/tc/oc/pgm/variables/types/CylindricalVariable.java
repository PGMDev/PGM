package tc.oc.pgm.variables.types;

import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.World;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.features.StateHolder;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.regions.Bounds;
import tc.oc.pgm.regions.CylindricalRegion;
import tc.oc.pgm.variables.Variable;
import tc.oc.pgm.variables.VariablesMatchModule;

public class CylindricalVariable extends AbstractVariable<Match>
    implements Variable.Indexed<Match>, StateHolder<CylindricalRegion.Mutable>, RegionDefinition {

  private static final Component[] COMPONENTS = Component.values();
  private final CylindricalRegion cylinder;
  private Cached lastLookup = null;

  public CylindricalVariable(CylindricalRegion cylinder) {
    super(Match.class);
    this.cylinder = cylinder;
  }

  @Override
  public void load(Match match) {
    match.getFeatureContext().registerState(this, cylinder.asMutableCopy());
  }

  @Override
  public boolean isDynamic() {
    return false;
  }

  record Cached(CylindricalRegion.Mutable cylinder, String world) {}

  private CylindricalRegion.Mutable getCylinder(World world) {
    var cached = lastLookup;
    if (cached == null || !cached.world.equals(world.getName())) {
      var match = PGM.get().getMatchManager().getMatch(world);
      if (match == null) throw new IllegalStateException("Couldn't find match");
      cached = lastLookup = new Cached(match.state(this), world.getName());
    }
    return cached.cylinder;
  }

  public int checkBounds(int idx, Filterable<?> obj) {
    if (idx < 0 || idx >= COMPONENTS.length) {
      String id = obj.moduleRequire(VariablesMatchModule.class).getId(this);
      PGM.get()
          .getGameLogger()
          .log(Level.SEVERE, String.format("Index %d out of bounds for cylinder %s", idx, id));
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
    return getCylinder(point.getWorld()).contains(point.toVector());
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    return match.state(this);
  }

  @Override
  public Bounds getBounds() {
    return cylinder.getBounds();
  }

  @Override
  public boolean isBlockBounded() {
    return cylinder.isBlockBounded();
  }

  @Override
  public boolean canGetRandom() {
    return cylinder.canGetRandom();
  }

  public enum Component {
    BASE_X,
    BASE_Y,
    BASE_Z,
    RADIUS,
    HEIGHT;

    private final ToDoubleFunction<CylindricalRegion.Mutable> getter =
        switch (this) {
          case BASE_X -> c -> c.getMutableBase().getX();
          case BASE_Y -> c -> c.getMutableBase().getY();
          case BASE_Z -> c -> c.getMutableBase().getZ();
          case RADIUS -> CylindricalRegion.Mutable::getMutableRadius;
          case HEIGHT -> CylindricalRegion.Mutable::getMutableHeight;
        };

    private final ObjDoubleConsumer<CylindricalRegion.Mutable> setter =
        switch (this) {
          case BASE_X -> (c, v) -> c.getMutableBase().setX(v);
          case BASE_Y -> (c, v) -> c.getMutableBase().setY(v);
          case BASE_Z -> (c, v) -> c.getMutableBase().setZ(v);
          case RADIUS -> CylindricalRegion.Mutable::setRadius;
          case HEIGHT -> CylindricalRegion.Mutable::setHeight;
        };
  }

  public double getComponent(Filterable<?> match, Component component) {
    return component.getter.applyAsDouble(match.state(this));
  }

  public void setComponent(Filterable<?> match, Component component, double value) {
    component.setter.accept(match.state(this), value);
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
