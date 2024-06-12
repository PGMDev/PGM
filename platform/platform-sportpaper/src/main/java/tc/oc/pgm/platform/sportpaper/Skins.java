package tc.oc.pgm.platform.sportpaper;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import tc.oc.pgm.util.skin.Skin;

public abstract class Skins {
  public static Skin fromProperties(PropertyMap profile) {
    for (Property property : profile.get("textures")) {
      if (property.hasSignature()) {
        return new Skin(property.getValue(), property.getSignature());
      } else {
        return new Skin(property.getValue(), null);
      }
    }
    return Skin.EMPTY;
  }

  public static Property toProperty(Skin skin) {
    if (skin == null || skin.isEmpty()) return null;

    if (skin.getSignature() != null) {
      return new Property("textures", skin.getData(), skin.getSignature());
    } else {
      return new Property("textures", skin.getData());
    }
  }

  public static PropertyMap toProperties(Skin skin) {
    PropertyMap map = new PropertyMap();
    if (skin != null && !skin.isEmpty()) {
      map.put("textures", toProperty(skin));
    }
    return map;
  }
}
