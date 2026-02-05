package tc.oc.pgm.platform.modern.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import tc.oc.pgm.util.skin.Skin;

public abstract class Skins {
  public static Skin fromProfile(GameProfile profile) {
    for (Property property : profile.properties().get("textures")) {
      return new Skin(property.value(), property.signature());
    }
    return Skin.EMPTY;
  }

  public static void toProfile(GameProfile profile, Skin skin) {
    if (skin == null || skin.isEmpty()) return;
    profile
        .properties()
        .put(
            "textures",
            skin.signature() != null
                ? new Property("textures", skin.data(), skin.signature())
                : new Property("textures", skin.data()));
  }
}
