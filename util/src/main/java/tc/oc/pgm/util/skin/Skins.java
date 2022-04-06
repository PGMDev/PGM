package tc.oc.pgm.util.skin;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import tc.oc.pgm.api.skin.Skin;

public abstract class Skins {
  public static SkinImpl fromProperties(PropertyMap profile) {
    for (Property property : profile.get("textures")) {
      if (property.hasSignature()) {
        return new SkinImpl(property.getValue(), property.getSignature());
      } else {
        return new SkinImpl(property.getValue(), null);
      }
    }
    return SkinImpl.EMPTY;
  }

  public static Property toProperty(Skin skin) {
    if (skin == null || skin.isEmpty()) return null;

    if (skin.getSignature() != null) {
      return new Property("textures", skin.getData(), skin.getSignature());
    } else {
      return new Property("textures", skin.getData());
    }
  }

  public static PropertyMap setProperties(Skin skin, PropertyMap properties) {
    properties.removeAll("textures");
    if (skin != null && !skin.isEmpty()) {
      properties.put("textures", toProperty(skin));
    }
    return properties;
  }

  public static PropertyMap toProperties(Skin skin) {
    return setProperties(skin, new PropertyMap());
  }
}
