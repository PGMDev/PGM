package tc.oc.pgm.api.attribute;

import java.util.UUID;

public interface AttributeModifier {
  UUID getUniqueId();

  String getName();

  double getAmount();

  Operation getOperation();

  /** Enumerable operation to be applied. */
  public enum Operation {

    /** Adds (or subtracts) the specified amount to the base value. */
    ADD_NUMBER,
    /** Adds this scalar of amount to the base value. */
    ADD_SCALAR,
    /** Multiply amount by this value, after adding 1 to it. */
    MULTIPLY_SCALAR_1;

    public static AttributeModifier.Operation fromOpcode(int code) {
      if (code < 0) code = 0;
      if (code >= values().length) code = values().length - 1;
      return values()[code];
    }
  }
}
