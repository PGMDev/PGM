package tc.oc.pgm.util.skin;

import java.util.Objects;

/**
 * A self-contained skin
 *
 * @param data The base64 encoded data for this skin, or null if this is the empty skin i.e.
 *     Steve/Alex
 * @param signature The base64 encoded signature for this skin, or null if this skin has no
 *     signature
 */
public record Skin(String data, String signature) {
  public static final Skin EMPTY = new Skin(null, null);

  @Deprecated
  public String getData() {
    return data();
  }

  @Deprecated
  public String getSignature() {
    return signature();
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

  @Override
  public int hashCode() {
    int result = data != null ? data.hashCode() : 0;
    result = 31 * result + (signature != null ? signature.hashCode() : 0);
    return result;
  }
}
