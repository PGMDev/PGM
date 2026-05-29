package tc.oc.pgm.platform.modern.modules.gamerules;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ModernRemappedGameRule<T> extends ModernBukkitGameRule<T> {
  private final String oldName;

  public ModernRemappedGameRule(org.bukkit.GameRule<T> handle, String oldName) {
    super(handle);
    this.oldName = oldName;
  }

  @Override
  public String name() {
    return oldName + " -> " + handle.getKey().asString();
  }

  @Override
  public T tryParse(String value) {
    // Legacy remapped game rules need to be manually parsed
    Object parsedValue;
    if (handle.getType() == Boolean.class) {
      parsedValue = Boolean.valueOf(value);
    } else if (handle.getType() == Integer.class) {
      parsedValue = Integer.valueOf(value);
    } else if (handle.getType() == String.class) {
      parsedValue = value;
    } else {
      throw new IllegalArgumentException(
          "Don't know how to parse value for " + handle.getType().getName());
    }
    return handle.getType().cast(parsedValue);
  }
}
