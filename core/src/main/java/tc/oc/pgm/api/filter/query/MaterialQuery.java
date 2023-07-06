package tc.oc.pgm.api.filter.query;

import tc.oc.pgm.util.nms.material.MaterialData;

public interface MaterialQuery extends Query {
  MaterialData getMaterial();
}
