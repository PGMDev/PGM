package tc.oc.bossbar;

import org.bukkit.entity.Player;
import tc.oc.component.Component;
import tc.oc.util.components.Components;

public class StaticBossBar implements BossBar {

  private final Component text;
  private final float meter;

  public StaticBossBar(Component text, float meter) {
    this.text = text;
    this.meter = meter;
  }

  @Deprecated
  public StaticBossBar(String text, float meter) {
    this(Components.fromLegacyText(text), meter);
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
