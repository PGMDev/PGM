package tc.oc.pgm.regions;

import net.kyori.adventure.text.Component;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.kits.Kit;

public class RegionFilterApplication implements FeatureDefinition {
  public final RFAScope scope;
  public final Region region;
  public final Filter filter;
  public final Kit kit;
  public final boolean lendKit;
  public final Vector velocity;
  public final @Nullable Component message;
  public final boolean earlyWarning;
  public boolean useRegionPriority;

  private RegionFilterApplication(
      RFAScope scope,
      Region region,
      Filter filter,
      Kit kit,
      boolean lendKit,
      Vector velocity,
      @Nullable Component message,
      boolean earlyWarning) {
    this.scope = scope;
    this.region = region;
    this.filter = filter;
    this.kit = kit;
    this.lendKit = lendKit;
    this.velocity = velocity;
    this.message = message;
    this.earlyWarning = earlyWarning;
  }

  public RegionFilterApplication(
      RFAScope scope,
      Region region,
      Filter filter,
      @Nullable Component message,
      boolean earlyWarning) {
    this(scope, region, filter, null, false, null, message, earlyWarning);
  }

  public RegionFilterApplication(
      RFAScope scope, Region region, Filter filter, Kit kit, boolean lendKit) {
    this(scope, region, filter, kit, lendKit, null, null, false);
  }

  public RegionFilterApplication(RFAScope scope, Region region, Filter filter, Vector velocity) {
    this(scope, region, filter, null, false, velocity, null, false);
  }
}
