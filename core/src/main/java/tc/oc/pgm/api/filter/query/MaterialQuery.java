package tc.oc.pgm.api.filter.query;

import tc.oc.pgm.util.material.MaterialData;

public interface MaterialQuery extends Query {
  MaterialData getMaterial();
}
