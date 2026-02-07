package tc.oc.pgm.platform.modern.modules.gamerules;

import static tc.oc.pgm.util.Assert.assertNotNull;

import net.minecraft.util.NullOps;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftGameRule;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.bukkit.GameRule;

@NullMarked
public class ModernBukkitGameRule<T> implements GameRule<T> {
  protected final org.bukkit.GameRule<T> handle;

  public ModernBukkitGameRule(org.bukkit.GameRule<T> handle) {
    this.handle = handle;
  }

  @Override
  public String name() {
    return handle.getKey().asString();
  }

  @Override
  public Class<T> type() {
    return handle.getType();
  }

  @Override
  public @Nullable T get(World world) {
    if (!world.getFeatureFlags().containsAll(handle.requiredFeatures()))
      // Avoid error if trying to get an unsupported experimental game rule
      return null;
    return world.getGameRuleValue(handle);
  }

  @Override
  public void set(World world, @Nullable T value) {
    if (!world.getFeatureFlags().containsAll(handle.requiredFeatures()))
      // Avoid errors if trying to get an unsupported experimental game rule
      return;
    if (value == null) value = world.getGameRuleDefault(handle);
    world.setGameRule(
        handle, assertNotNull(value, "value is null and the game rule has no default"));
  }

  @Override
  public T tryParse(String value) throws Throwable {
    var nms = ((CraftGameRule<T>) handle).getHandle();
    return nms.deserialize(value)
        .flatMap(val ->
            // The game rule may have additional validation in place. Validate it here so the
            // mistake can be caught early.
            nms.valueCodec()
                .encodeStart(NullOps.INSTANCE, val)
                .map(ignored -> val)
                .mapError(error -> "Invalid game rule value: " + error))
        .getOrThrow(Exception::new);
  }

  @Override
  public boolean canBeCombinedWith(GameRule<?> other) {
    return !(other instanceof ModernBukkitGameRule<?> bukkitRule
        && bukkitRule.handle.getKey().equals(handle.getKey()));
  }
}
