package tc.oc.pgm.rotation.pools;

import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.RandomMapOrder;

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
}
