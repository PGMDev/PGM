package tc.oc.pgm.modes;

import static net.kyori.adventure.text.Component.text;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

@FeatureInfo(name = "mode")
public class Mode extends SelfIdentifyingFeatureDefinition {
  private final MaterialData material;
  private final Duration after;
  private final @Nullable Filter filter;
  private final @Nullable String name;
  private final String legacyName;
  private final Component componentName;
  private final Duration showBefore;

  public Mode(final MaterialData material, final Duration after, Duration showBefore) {
    this(null, material, after, null, null, showBefore);
  }

  public Mode(
      final @Nullable String id,
      final MaterialData material,
      final Duration after,
      final @Nullable Filter filter,
      final @Nullable String name,
      Duration showBefore) {
    super(id);
    this.material = material;
    this.after = after;
    this.filter = filter;
    this.name = name;
    this.legacyName = name != null ? name : getPreformattedMaterialName();
    this.componentName = text(legacyName, NamedTextColor.RED);
    this.showBefore = showBefore;
  }

  @Override
  protected String getDefaultId() {
    return makeDefaultId() + "--" + makeId(legacyName);
  }

  public MaterialData getMaterialData() {
    return this.material;
  }

  public String getLegacyName() {
    return legacyName;
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

  public @Nullable Filter getFilter() {
    return this.filter;
  }

  public @Nullable String getName() {
    return this.name;
  }

  public static String makeDefaultId(@Nullable String name, AtomicInteger serial) {
    return "--" + makeTypeName(Mode.class) + "-" + makeId(name);
  }
}
