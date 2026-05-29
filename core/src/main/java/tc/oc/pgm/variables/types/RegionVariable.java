package tc.oc.pgm.variables.types;

import java.util.Map;
import java.util.WeakHashMap;
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

public abstract class RegionVariable<
        R extends RegionDefinition.Mutable, I extends Region & RegionDefinition.MutableSource<R>>
    extends AbstractVariable<Match>
    implements Variable.Indexed<Match>, StateHolder<R>, RegionDefinition {

  private final Component<R>[] components;
  protected final I initial;
  private final Map<Match, R> states = new WeakHashMap<>();

  protected RegionVariable(Component<R>[] components, I initial) {
    super(Match.class);
    this.components = components;
    this.initial = initial;
  }

  protected R createState() {
    return initial.asMutableCopy();
  }

  @Override
  public void load(Match match) {
    states.put(match, createState());
  }

  private R getState(Match match) {
    return states.get(match);
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    return (Region.Static) getState(match);
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
    if (index < 0 || index >= components.length) {
      String msg =
          String.format("Index %d out of bounds for %s", index, getClass().getSimpleName());
      PGM.get().getGameLogger().log(Level.SEVERE, msg);
      return 0;
    }
    return components[index].getter().applyAsDouble(getState(getAncestor(context)));
  }

  @Override
  public void setValue(Filterable<?> context, int index, double value) {
    if (index < 0 || index >= components.length) {
      String msg =
          String.format("Index %d out of bounds for %s", index, getClass().getSimpleName());
      PGM.get().getGameLogger().log(Level.SEVERE, msg);
      return;
    }
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
  public boolean canGetRandom() {
    return initial.canGetRandom();
  }

  @Override
  public boolean isDynamic() {
    return false;
  }

  protected Variable<Match> getComponent(Component<R> component) {
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
