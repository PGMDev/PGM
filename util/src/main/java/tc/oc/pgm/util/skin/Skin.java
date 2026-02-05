package tc.oc.pgm.util.skin;

import java.util.Objects;

/** A self-contained skin */
public record Skin(String data, String signature) {
  public static final Skin EMPTY = new Skin(null, null);

  /**
   * Return the base64 encoded data for this skin, or null if this is the empty skin i.e. Steve/Alex
   */
  @Override
  public String data() {
    return data;
  }

  /** Return the base64 encoded signature for this skin, or null if this skin has no signature */
  @Override
  public String signature() {
    return signature;
  }

  /** Return true if this is the empty skin i.e. Steve/Alex */
  public boolean isEmpty() {
    return this.data == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Skin(String skinData, String skinSignature))) {
      return false;
    }
    if (!Objects.equals(data, skinData)) {
      return false;
    }
    return Objects.equals(signature, skinSignature);
  }
}
