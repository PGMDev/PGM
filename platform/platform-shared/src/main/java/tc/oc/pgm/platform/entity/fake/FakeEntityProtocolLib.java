package tc.oc.pgm.platform.entity.fake;

import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.nms.entity.fake.FakeEntity;

public abstract class FakeEntityProtocolLib implements FakeEntity {
  private final int entityID = NMSHacks.allocateEntityId();

  @Override
  public int entityId() {
    return entityID;
  }
}
