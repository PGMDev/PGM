package tc.oc.pgm.platform.sportpaper.material;

import org.bukkit.Material;

@SuppressWarnings("deprecation")
class SpEncodeUtil {
  private static final int ENCODED_NULL_MATERIAL = -1;

  static int encode(Material mat, int data) {
    return encode(mat.getId(), data);
  }

  static int encode(int typeId, int data) {
    return typeId + (data << 12);
  }

  static SpMaterialData decode(int encoded) {
    if (encoded == ENCODED_NULL_MATERIAL) return null;
    return new SpMaterialData(decodeMaterial(encoded), decodeData(encoded));
  }

  static Material decodeMaterial(int encoded) {
    return Material.getMaterial(encoded & 0xfff);
  }

  static byte decodeData(int encoded) {
    return (byte) (encoded >> 12);
  }
}
