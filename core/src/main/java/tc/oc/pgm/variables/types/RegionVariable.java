package tc.oc.pgm.variables.types;

import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.Location;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.features.StateHolder;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.regions.Bounds;
import tc.oc.pgm.regions.Component;
import tc.oc.pgm.variables.Variable;

public abstract class RegionVariable<R extends RegionDefinition.Mutable & Region.Static>
    extends AbstractVariable<Match>
    implements Variable.Indexed<Match>, StateHolder<R>, RegionDefinition {

  private final Component<R>[] components;
  private final Region initial;
  private final Map<Match, R> states = new WeakHashMap<>();

  protected RegionVariable(Component<R>[] components, Region initial) {
    super(Match.class);
    this.components = components;
    this.initial = initial;
  }

  protected abstract R createState();

  @Override
  public void load(Match match) {
    states.put(match, createState());
  }

  private R getState(Match match) {
    return states.get(match);
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    return getState(match);
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

  @Override
  public double getValue(Filterable<?> context, int index) {
    return components[index].getter().applyAsDouble(getState(getAncestor(context)));
  }

  @Override
  public void setValue(Filterable<?> context, int index, double value) {
    components[index].setter().accept(getState(getAncestor(context)), value);
  }

  @Override
  protected double getValueImpl(Match match) {
    throw new UnsupportedOperationException("Use indexed getValue");
  }

  @Override
  protected void setValueImpl(Match match, double value) {
    throw new UnsupportedOperationException("Use indexed setValue");
  }

  @Override
  public int size() {
    return components.length;
  }

  @Override
  public boolean isDynamic() {
    return Variable.Indexed.super.isDynamic();
  }

  protected Variable<Match> getComponent(tc.oc.pgm.regions.Component<R> component) {
    return new AbstractVariable<>(Match.class) {
      @Override
      protected double getValueImpl(Match match) {
        return component.getter().applyAsDouble(getState(match));
      }

      @Override
      protected void setValueImpl(Match match, double value) {
        component.setter().accept(getState(match), value);
      }
    };
  }
}
