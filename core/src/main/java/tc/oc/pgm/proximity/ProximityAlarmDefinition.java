package tc.oc.pgm.proximity;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;

public class ProximityAlarmDefinition {
  public Region detectRegion; // Region in which players are detected
  public Filter detectFilter; // Players that are detected

  public Filter alertFilter; // Players that are alerted
  public String alertMessage; // Message to send to eligible players

  public boolean flares; // Show flares
  public double flareRadius; // Radius of flare circle
}
