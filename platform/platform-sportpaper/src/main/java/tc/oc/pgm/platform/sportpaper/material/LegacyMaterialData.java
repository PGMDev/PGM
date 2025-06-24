package tc.oc.pgm.platform.sportpaper.material;

import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;

public interface LegacyMaterialData extends MaterialData {

  byte getData();

  @Override
  default MaterialMatcher toMatcher() {
    return new ExactMaterialMatcher(getItemType(), getData());
  }
}
