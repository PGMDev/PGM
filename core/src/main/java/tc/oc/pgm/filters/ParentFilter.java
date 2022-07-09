package tc.oc.pgm.filters;

import java.util.List;
import tc.oc.pgm.api.filter.Filter;

/** A filter that has children filters for any reason. */
public interface ParentFilter extends Filter {

  /** Get the children filters */
  List<Filter> getChildren();
}
