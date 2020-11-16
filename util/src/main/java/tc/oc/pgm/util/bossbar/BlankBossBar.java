package tc.oc.pgm.util.bossbar;

import static net.kyori.adventure.text.Component.empty;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class BlankBossBar implements BossBarSource {

  public static final BlankBossBar INSTANCE = new BlankBossBar();

  private BlankBossBar() {}

  @Override
  public boolean isVisible(Player viewer) {
    return false;
  }

  @Override
  public Component getText(Player viewer) {
    return empty();
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
