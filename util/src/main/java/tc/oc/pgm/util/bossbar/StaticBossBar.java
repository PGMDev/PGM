package tc.oc.pgm.util.bossbar;

import net.kyori.text.Component;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.text.TextParser;

public class StaticBossBar implements BossBarSource {

  private final Component text;
  private final float meter;

  public StaticBossBar(Component text, float meter) {
    this.text = text;
    this.meter = meter;
  }

  @Deprecated
  public StaticBossBar(String text, float meter) {
    this(TextParser.parseComponent(text), meter);
  }

  @Override
  public boolean isVisible(Player viewer) {
    return true;
  }

  @Override
  public Component getText(Player viewer) {
    return text;
  }

  @Override
  public float getMeter(Player viewer) {
    return meter;
  }

  @Override
  public void addObserver(BossBarObserver observer) {}

  @Override
  public void removeObserver(BossBarObserver observer) {}
}
