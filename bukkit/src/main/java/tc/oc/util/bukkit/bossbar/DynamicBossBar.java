package tc.oc.util.bukkit.bossbar;

import java.util.HashSet;
import java.util.Set;

public abstract class DynamicBossBar implements BossBar {

  private final Set<BossBarObserver> observers = new HashSet<>();

  public void invalidate() {
    for (BossBarObserver observer : observers) {
      observer.invalidate(this);
    }
  }

  @Override
  public void addObserver(BossBarObserver observer) {
    observers.add(observer);
  }

  @Override
  public void removeObserver(BossBarObserver observer) {
    observers.remove(observer);
  }
}
