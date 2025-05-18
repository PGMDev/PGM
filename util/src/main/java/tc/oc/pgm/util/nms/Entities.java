package tc.oc.pgm.util.nms;

import tc.oc.pgm.util.nms.entities.CustomEntities;
import tc.oc.pgm.util.platform.Platform;

public interface Entities {
    CustomEntities CUSTOM_ENTITIES = Platform.get(CustomEntities.class);
}
