package tc.oc.pgm.util.skin;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.util.EnumSet;
import java.util.Set;

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

  public static Set<Skin.Part> partsFromFlags(int flags) {
    EnumSet<Skin.Part> parts = EnumSet.noneOf(Skin.Part.class);
    for (Skin.Part part : Skin.Part.values()) {
      if ((flags & (1 << part.ordinal())) != 0) {
        parts.add(part);
      }
    }
    return parts;
  }

  public static int flagsFromParts(Iterable<Skin.Part> parts) {
    int flags = 0;
    for (Skin.Part part : parts) {
      flags |= 1 << part.ordinal();
    }
    return flags;
  }
}
