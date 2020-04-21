package tc.oc.pgm.util.bossbar;

import org.bukkit.entity.Player;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.BlankComponent;

public class BlankBossBar implements BossBar {

  public static final BlankBossBar INSTANCE = new BlankBossBar();

  private BlankBossBar() {}

  @Override
  public boolean isVisible(Player viewer) {
    return false;
  }

  @Override
  public Component getText(Player viewer) {
    return BlankComponent.INSTANCE;
  }

  @Override
  public float getMeter(Player viewer) {
    return 0;
  }

  @Override
  public void addObserver(BossBarObserver observer) {}

  @Override
  public void removeObserver(BossBarObserver observer) {}
}
