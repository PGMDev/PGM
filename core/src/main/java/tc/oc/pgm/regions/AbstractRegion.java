package tc.oc.pgm.regions;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.filter.query.LocationQuery;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.filters.TypedFilter;
import tc.oc.pgm.util.block.BlockVectors;

public abstract class AbstractRegion extends TypedFilter<LocationQuery>
    implements RegionDefinition {

  @Override
  public boolean contains(Location point) {
    return this.contains(point.toVector());
  }

  @Override
  public boolean contains(BlockVector blockPos) {
    return this.contains((Vector) BlockVectors.center(blockPos));
  }

  @Override
  public boolean contains(Block block) {
    return this.contains(BlockVectors.center(block));
  }

  @Override
  public boolean contains(BlockState block) {
    return this.contains(BlockVectors.center(block));
  }

  @Override
  public boolean contains(Entity entity) {
    return this.contains(entity.getLocation().toVector());
  }

  @Override
  public boolean enters(Location from, Location to) {
    return !this.contains(from) && this.contains(to);
  }

  @Override
  public boolean enters(Vector from, Vector to) {
    return !this.contains(from) && this.contains(to);
  }

  @Override
  public boolean exits(Location from, Location to) {
    return this.contains(from) && !this.contains(to);
  }

  @Override
  public boolean exits(Vector from, Vector to) {
    return this.contains(from) && !this.contains(to);
  }

  @Override
  public boolean canGetRandom() {
    return false;
  }

  @Override
  public boolean isBlockBounded() {
    return false;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public Iterator<BlockVector> getBlockVectorIterator() {
    return Iterators.filter(
        this.getBounds().getBlockIterator(),
        new Predicate<BlockVector>() {
          @Override
          public boolean apply(BlockVector vector) {
            return contains(vector);
          }
        });
  }

  @Override
  public Iterable<BlockVector> getBlockVectors() {
    return new Iterable<BlockVector>() {
      @Override
      public Iterator<BlockVector> iterator() {
        return getBlockVectorIterator();
      }
    };
  }

  @Override
  public Vector getRandom(Random random) {
    throw new UnsupportedOperationException(
        "Cannot generate a random point in " + this.getClass().getSimpleName());
  }

  @Override
  public Class<? extends LocationQuery> getQueryType() {
    return LocationQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(LocationQuery query) {
    return QueryResponse.fromBoolean(contains(query.getLocation()));
  }
}
