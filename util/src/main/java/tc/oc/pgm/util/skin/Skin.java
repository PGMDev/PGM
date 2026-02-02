package tc.oc.pgm.util.skin;

import java.util.Objects;

/** A self-contained skin */
public class Skin {
  public static final Skin EMPTY = new Skin(null, null);

  private final String data;
  private final String signature;

  public Skin(String data, String signature) {
    this.data = data;
    this.signature = signature;
  }

  /**
   * Return the base64 encoded data for this skin, or null if this is the empty skin i.e. Steve/Alex
   */
  public String getData() {
    return data;
  }

  /** Return the base64 encoded signature for this skin, or null if this skin has no signature */
  public String getSignature() {
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
    if (!(o instanceof Skin skin)) {
      return false;
    }
    if (!Objects.equals(data, skin.data)) {
      return false;
    }
    return Objects.equals(signature, skin.signature);
  }

  @Override
  public int hashCode() {
    int result = data != null ? data.hashCode() : 0;
    result = 31 * result + (signature != null ? signature.hashCode() : 0);
    return result;
  }
}
