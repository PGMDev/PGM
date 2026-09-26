package tc.oc.pgm.variables.types;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.variables.LocalFrame;
import tc.oc.pgm.variables.Variable;

public class LocalVariable implements Variable<Match> {

  private final String name;
  private final LocalFrame.Layout layout;
  private final int slot;

  public LocalVariable(String name, LocalFrame.Layout layout, int slot) {
    this.name = name;
    this.layout = layout;
    this.slot = slot;
  }

  public String getName() {
    return name;
  }

  @Override
  public double getValue(Filterable<?> context) {
    return LocalFrame.values(layout, name)[slot];
  }

  @Override
  public void setValue(Filterable<?> context, double value) {
    LocalFrame.values(layout, name)[slot] = value;
  }

  @Override
  public Class<Match> getScope() {
    return Match.class;
  }

  @Override
  public String toString() {
    return "LocalVariable{name=" + name + ", slot=" + slot + '}';
  }
}
