package tc.oc.pgm.platform.sportpaper.modules.gamerules;

import java.util.function.Function;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.bukkit.GameRule;

@NullMarked
public class SpRemappedGameRule<T, U> implements GameRule<T> {
  private final String modernName;
  private final GameRule<U> backingRule;
  private final Class<T> ruleType;
  private final Function<@Nullable U, @Nullable T> get;
  private final Function<@Nullable T, @Nullable U> set;

  public SpRemappedGameRule(
      String modernName,
      GameRule<U> backingRule,
      Class<T> ruleType,
      Function<@Nullable U, @Nullable T> get,
      Function<@Nullable T, @Nullable U> set) {
    this.modernName = modernName;
    this.backingRule = backingRule;
    this.ruleType = ruleType;
    this.get = get;
    this.set = set;
  }

  @Override
  public String name() {
    return "minecraft:" + modernName + " -> " + backingRule.name();
  }

  @Override
  public Class<T> type() {
    return ruleType;
  }

  @Override
  public @Nullable T get(World world) {
    return get.apply(backingRule.get(world));
  }

  @Override
  public void set(World world, @Nullable T value) {
    backingRule.set(world, value != null ? set.apply(value) : null);
  }

  @Override
  public T tryParse(String value) {
    return SpKVGameRule.parseStringValue(value, ruleType);
  }

  @Override
  public boolean canBeCombinedWith(GameRule<?> other) {
    return other != backingRule
        && !(other instanceof SpRemappedGameRule<?, ?> remapped
            && remapped.backingRule == this.backingRule);
  }

  protected GameRule<U> backingRule() {
    return backingRule;
  }
}
