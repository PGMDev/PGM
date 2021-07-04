package tc.oc.pgm.filters.query;

import tc.oc.pgm.flag.Flag;

public class FlagQuery extends GoalQuery {
  private final Flag flag;

  public FlagQuery(Flag flag) {
    super(flag);
    this.flag = flag;
  }

  public Flag getFlag() {
    return flag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FlagQuery)) return false;
    if (!super.equals(o)) return false;
    FlagQuery query = (FlagQuery) o;
    if (!flag.equals(query.flag)) return false;
    return true;
  }
}
