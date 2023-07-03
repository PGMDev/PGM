package tc.oc.pgm.util.nms.entity.fake;

import tc.oc.pgm.util.nms.NMSHacks;

public abstract class FakeEntityProtocolLib implements FakeEntity {
  private final int entityID = NMSHacks.allocateEntityId();

  @Override
  public int entityId() {
    return entityID;
  }
}
