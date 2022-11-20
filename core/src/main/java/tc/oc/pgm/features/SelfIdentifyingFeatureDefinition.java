package tc.oc.pgm.features;

import java.text.Normalizer;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureInfo;

/** A {@link FeatureDefinition} that knows its own ID */
public abstract class SelfIdentifyingFeatureDefinition implements FeatureDefinition {
  private final @Nullable String id;

  public SelfIdentifyingFeatureDefinition(@Nullable String id) {
    this.id = id;
  }

  protected String makeDefaultId() {
    return "--" + this.getTypeName();
  }

  protected @Nullable String getDefaultId() {
    return null;
  }

  public String getId() {
    return this.id != null ? this.id : this.getDefaultId();
  }

  public String getTypeName() {
    return makeTypeName(this.getClass());
  }

  /** Get a readable name for the feature represented by the given type */
  public static String makeTypeName(Class<? extends FeatureDefinition> type) {
    FeatureInfo info = type.getAnnotation(FeatureInfo.class);
    if (info != null) {
      return info.name();
    } else {
      return type.getSimpleName();
    }
  }

  public static String makeId(String text) {
    return Normalizer.normalize(ChatColor.stripColor(text), Normalizer.Form.NFD)
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\s+", "-");
  }
}
