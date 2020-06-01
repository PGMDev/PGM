package tc.oc.pgm.modes;

import java.time.Duration;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.material.MaterialData;

public class Mode {

  private final MaterialData material;
  private final Duration after;
  private final @Nullable String name;
  private final Component componentName;
  private final Duration showBefore;

  public Mode(final MaterialData material, final Duration after, Duration showBefore) {
    this(material, after, null, showBefore);
  }

  public Mode(
      final MaterialData material,
      final Duration after,
      final @Nullable String name,
      Duration showBefore) {
    this.material = material;
    this.after = after;
    this.name = name;
    this.componentName =
        TextComponent.of(name != null ? name : getPreformattedMaterialName(), TextColor.RED);
    this.showBefore = showBefore;
  }

  public MaterialData getMaterialData() {
    return this.material;
  }

  public Component getComponentName() {
    return componentName;
  }

  public String getPreformattedMaterialName() {
    return ModeUtils.formatMaterial(this.material);
  }

  public Duration getAfter() {
    return this.after;
  }

  public Duration getShowBefore() {
    return this.showBefore;
  }

  public @Nullable String getName() {
    return this.name;
  }
}
