package tc.oc.pgm.api.filter;

import java.util.List;

/** A filter that has children filters for any reason. */
public interface ParentFilter extends Filter {

  /** Get the children filters */
  List<Filter> getChildren();
}
