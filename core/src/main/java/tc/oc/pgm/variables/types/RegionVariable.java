package tc.oc.pgm.variables.types;

import java.util.logging.Level;
import org.bukkit.Location;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.features.StateHolder;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.regions.Bounds;
import tc.oc.pgm.regions.Component;
import tc.oc.pgm.variables.Variable;
import tc.oc.pgm.variables.VariablesMatchModule;

public abstract class RegionVariable<
        R extends RegionDefinition.Mutable, I extends Region & RegionDefinition.MutableSource<R>>
    extends AbstractVariable<Match>
    implements Variable.Indexed<Match>, StateHolder<R>, RegionDefinition {

  private final Component<R>[] components;
  protected final I initial;

  protected RegionVariable(Component<R>[] components, I initial) {
    super(Match.class);
    this.components = components;
    this.initial = initial;
  }

  @Override
  public void load(Match match) {
    match.getFeatureContext().registerState(this, initial.asMutableCopy());
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    return (Region.Static) match.state(this);
  }

  @Override
  public boolean contains(Location point) {
    return getStatic(point.getWorld()).contains(point);
  }

  @Override
  public Bounds getBounds() {
    return initial.getBounds();
  }

  @Override
  public boolean isBlockBounded() {
    return initial.isBlockBounded();
  }

  public int checkBounds(int idx, Filterable<?> obj) {
    if (idx < 0 || idx >= components.length) {
      String id = obj.moduleRequire(VariablesMatchModule.class).getId(this);
      PGM.get()
          .getGameLogger()
          .log(Level.SEVERE, String.format("Index %d out of bounds for variable: %s", idx, id));
      return 0;
    }
    return idx;
  }

  @Override
  public double getValue(Filterable<?> context, int idx) {
    return components[checkBounds(idx, context)].getter().applyAsDouble(context.state(this));
  }

  @Override
  public void setValue(Filterable<?> context, int idx, double value) {
    components[checkBounds(idx, context)].setter().accept(context.state(this), value);
  }

  @Override
  protected double getValueImpl(Match match) {
    throw new UnsupportedOperationException("Use indexed getValue");
  }

  @Override
  protected void setValueImpl(Match match, double value) {
    throw new UnsupportedOperationException("Use indexed setValue");
  }

  public Component<R>[] getComponents() {
    return this.components;
  }

  @Override
  public int size() {
    return components.length;
  }

  @Override
  public boolean canGetRandom() {
    return initial.canGetRandom();
  }

  @Override
  public boolean isDynamic() {
    return false;
  }

  public Variable<Match> getComponent(Component<R> component) {
    return new AbstractVariable<>(Match.class) {
      @Override
      protected double getValueImpl(Match match) {
        return component.getter().applyAsDouble(match.state(RegionVariable.this));
      }

      @Override
      protected void setValueImpl(Match match, double value) {
        component.setter().accept(match.state(RegionVariable.this), value);
      }
    };
  }
}
