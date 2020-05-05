package tc.oc.pgm.rotation;

import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.map.MapInfo;

/* RandomMapPool - A map pool which utilizes an instance of {@link RandomMapOrder} */
public class RandomMapPool extends MapPool {

  private final RandomMapOrder order;

  public RandomMapPool(MapPoolManager manager, ConfigurationSection section, String name) {
    super(manager, section, name);
    this.order = new RandomMapOrder(maps);
  }

  @Override
  public MapInfo popNextMap() {
    return order.popNextMap();
  }

  @Override
  public MapInfo getNextMap() {
    return order.getNextMap();
  }

  @Override
  public void setNextMap(MapInfo map) {
    order.setNextMap(map);
  }

  @Override
  public void resetNextMap() {
    order.resetNextMap();
  }
}
