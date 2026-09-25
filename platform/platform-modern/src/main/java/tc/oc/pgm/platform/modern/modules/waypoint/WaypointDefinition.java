package tc.oc.pgm.platform.modern.modules.waypoint;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import org.bukkit.Color;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

public class WaypointDefinition extends SelfIdentifyingFeatureDefinition {
  private final ResourceKey<WaypointStyleAsset> style;
  private final Color color;
  private final @Nullable Float transmitRange;

  public WaypointDefinition(
      @Nullable String id,
      ResourceKey<WaypointStyleAsset> style,
      Color color,
      @Nullable Float transmitRange) {
    super(id);
    this.style = style;
    this.color = color;
    this.transmitRange = transmitRange;
  }

  public ResourceKey<WaypointStyleAsset> getStyle() {
    return style;
  }

  public Color getColor() {
    return color;
  }

  public @Nullable Float getTransmitRange() {
    return transmitRange;
  }
}
