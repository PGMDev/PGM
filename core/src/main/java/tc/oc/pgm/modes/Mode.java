package tc.oc.pgm.modes;

import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.material.MaterialData;
import org.joda.time.Duration;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedText;

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
        new PersonalizedText(name != null ? name : getPreformattedMaterialName(), ChatColor.RED);
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
