package tc.oc.pgm.modes;

import static net.kyori.adventure.text.Component.text;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.util.material.BlockMaterialData;

@FeatureInfo(name = "mode")
public class Mode extends SelfIdentifyingFeatureDefinition {
  private final @Nullable String name;
  private final String legacyName;
  private final Component componentName;

  private final Duration after;
  private final Duration showBefore;
  private final @Nullable Filter filter;
  private final @Nullable BlockMaterialData material;
  private final @Nullable Action<? super Match> action;

  public Mode(
      final String id,
      final @Nullable String name,
      final Duration after,
      final Duration showBefore,
      final @Nullable Filter filter,
      final @Nullable BlockMaterialData material,
      final @Nullable Action<? super Match> action) {
    super(id);
    this.name = name;
    this.legacyName = getName(() -> ModeUtils.formatMaterial(material));
    this.componentName = text(legacyName, NamedTextColor.RED);
    this.after = after;
    this.showBefore = showBefore;
    this.filter = filter;
    this.material = material;
    this.action = action;
  }

  public String getName(Supplier<String> ifNull) {
    return Objects.requireNonNullElseGet(this.name, ifNull);
  }

  public String getLegacyName() {
    return legacyName;
  }

  public Component getComponentName() {
    return componentName;
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

  public @Nullable BlockMaterialData getMaterialData() {
    return this.material;
  }

  public @Nullable Action<? super Match> getAction() {
    return this.action;
  }
}
