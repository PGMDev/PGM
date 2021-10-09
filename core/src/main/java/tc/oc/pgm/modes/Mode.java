package tc.oc.pgm.modes;

import static net.kyori.adventure.text.Component.text;

import java.time.Duration;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.CountdownContext;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.filters.dynamic.FilterMatchModule;

public class Mode extends SelfIdentifyingFeatureDefinition {
  private final MaterialData material;
  private final Duration after;
  private final Filter filter;
  private final @Nullable String name;
  private final Component componentName;
  private final Duration showBefore;
  private boolean modeComplete = false;

  public Mode(final MaterialData material, final Duration after, Duration showBefore) {
    this(null, material, after, null, null, showBefore);
  }

  public Mode(
      final @Nullable String id,
      final MaterialData material,
      final Duration after,
      final Filter filter,
      final @Nullable String name,
      Duration showBefore) {
    super(id);
    this.material = material;
    this.after = after;
    this.filter = filter;
    this.name = name;
    this.componentName =
        text(name != null ? name : getPreformattedMaterialName(), NamedTextColor.RED);
    this.showBefore = showBefore;
  }

  public void load(
      FilterMatchModule fmm,
      ModeChangeCountdown countdown,
      CountdownContext countdownContext,
      Match match) {
    // if filter returns ALLOW at any time in the match, start countdown for mode change
    fmm.onRise(
        filter,
        FilterListener -> {
          if (match.isRunning() && !modeComplete) {
            countdownContext.start(countdown, countdown.getMode().getAfter());
            modeComplete = true;
          }
        });
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

  public Filter getFilter() {
    return this.filter;
  }

  public boolean isModeComplete() {
    return this.modeComplete;
  }

  public @Nullable String getName() {
    return this.name;
  }

  public void setModeComplete() {
    this.modeComplete = true;
  }
}
