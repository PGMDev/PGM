package tc.oc.pgm.platform.sportpaper.modules.gamerules;

import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.server.v1_8_R3.GameRules;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.Result;
import tc.oc.pgm.util.bukkit.GameRule;

@NullMarked
public class SpKVGameRule<T> implements GameRule<T> {
  private static final Map<Class<?>, BiFunction<GameRules, String, ?>> READ_MAPPINGS = Map.of(
      String.class, GameRules::get,
      Integer.class, GameRules::c,
      Boolean.class, GameRules::getBoolean);

  protected final String ruleName;
  protected final Class<T> ruleType;

  public SpKVGameRule(String ruleName, Class<T> ruleType) {
    this.ruleName = ruleName;
    this.ruleType = ruleType;
  }

  @Override
  public String name() {
    return ruleName;
  }

  @Override
  public Class<T> type() {
    return ruleType;
  }

  @Override
  public @Nullable T get(World world) {
    // NMS parses game rule values internally (this isn't exposed in the Bukkit API). We are
    // leveraging this to read the effective value of the game rule.
    GameRules rules = ((CraftWorld) world).getHandle().getGameRules();
    BiFunction<GameRules, String, ?> reader = READ_MAPPINGS.get(ruleType);
    if (reader == null) {
      throw new IllegalStateException("Don't know how to read value for " + ruleType.getName());
    }
    return ruleType.cast(reader.apply(rules, ruleName));
  }

  @Override
  public void set(World world, @Nullable T value) {
    setFromString(world, value != null ? value.toString() : null);
  }

  @Override
  public void setFromString(World world, @Nullable String value) {
    if (value == null) value = SpGameRules.DEFAULTS.get(ruleName);
    world.setGameRuleValue(ruleName, value);
  }

  @Override
  public Result<T, ?> tryParse(String value) {
    return parseStringValue(value, ruleType);
  }

  @Override
  public boolean canBeCombinedWith(GameRule<?> other) {
    return switch (other) {
      case final SpKVGameRule<?> kvRule -> !this.ruleName.equals(kvRule.ruleName);
      case final SpRemappedGameRule<?, ?> remappedRule ->
        !this.ruleName.equals(remappedRule.backingRule().name());
      default -> true;
    };
  }

  protected static <T> Result<T, ?> parseStringValue(String value, Class<T> ruleType) {
    try {
      Object parsedValue;
      if (ruleType == Boolean.class) {
        parsedValue = Boolean.valueOf(value);
      } else if (ruleType == Integer.class) {
        parsedValue = Integer.valueOf(value);
      } else if (ruleType == String.class) {
        parsedValue = value;
      } else {
        return Result.err(new IllegalArgumentException(
            "Don't know how to parse value for " + ruleType.getName()));
      }

      try {
        return Result.ok(ruleType.cast(parsedValue));
      } catch (ClassCastException e) {
        return Result.err(e);
      }
    } catch (Throwable e) {
      return Result.err(e);
    }
  }
}
