package tc.oc.pgm.filters.query;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

public class EntityQuery extends Query implements tc.oc.pgm.api.filter.query.EntityQuery {

  private final Entity entity;

  public EntityQuery(@Nullable Event event, Entity entity) {
    super(event);
    this.entity = assertNotNull(entity);
  }

  public EntityQuery(Entity entity) {
    this(null, entity);
  }

  @Override
  public Class<? extends Entity> getEntityType() {
    return entity.getClass();
  }

  @Nullable
  @Override
  public Inventory getInventory() {
    return this.entity instanceof InventoryHolder
        ? ((InventoryHolder) this.entity).getInventory()
        : null;
  }

  @Override
  public Location getLocation() {
    return this.entity.getLocation();
  }

  @Override
  public Match getMatch() {
    return PGM.get().getMatchManager().getMatch(entity.getWorld());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EntityQuery)) return false;
    EntityQuery query = (EntityQuery) o;
    if (!entity.equals(query.entity)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return entity.hashCode();
  }
}
