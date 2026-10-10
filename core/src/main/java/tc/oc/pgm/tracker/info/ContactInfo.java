package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.CauseInfo;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.OwnerInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;

/** Damage from being in contact with a block, such as a cactus. */
public class ContactInfo implements OwnerInfo, CauseInfo, DamageInfo {

  public enum Type {
    CACTUS,
    HOT_FLOOR,
    CAMPFIRE,
    SWEET_BERRY_BUSH,
    STALAGMITE,
    OTHER
  }

  private final Type type;
  private final @Nullable PhysicalInfo block;

  public ContactInfo(Type type, @Nullable PhysicalInfo block) {
    this.type = assertNotNull(type);
    this.block = block;
  }

  public Type getType() {
    return type;
  }

  /** The block dealing the damage, which may not be known on legacy. */
  public @Nullable PhysicalInfo getBlock() {
    return block;
  }

  @Override
  public @Nullable PhysicalInfo getCause() {
    return getBlock();
  }

  @Override
  public @Nullable ParticipantState getOwner() {
    return block == null ? null : block.getOwner();
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return getOwner();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{type=" + type + " block=" + block + "}";
  }
}
