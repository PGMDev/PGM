package tc.oc.pgm.filters.query;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

/**
 * A block query is canonically defined by a {@link World} and a set of integer block coordinates.
 * The other properties are created lazily, to gain a bit of efficiency when querying filters that
 * don't check them.
 */
public class BlockQuery extends Query implements tc.oc.pgm.api.filter.query.BlockQuery {

  private final World world;
  private final int x, y, z;
  private @Nullable BlockState block;
  private @Nullable Location location;
  private @Nullable MaterialData material;

  public BlockQuery(@Nullable Event event, World world, int x, int y, int z) {
    super(event);
    this.world = checkNotNull(world);
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public BlockQuery(@Nullable Event event, World world, BlockVector pos) {
    this(event, world, checkNotNull(pos).getBlockX(), pos.getBlockY(), pos.getBlockZ());
  }

  public BlockQuery(@Nullable Event event, BlockState block) {
    this(event, checkNotNull(block).getWorld(), block.getX(), block.getY(), block.getZ());
    this.block = block;
  }

  public BlockQuery(@Nullable Event event, Block block) {
    this(event, block.getWorld(), block.getX(), block.getY(), block.getZ());
  }

  public BlockQuery(Block block) {
    this(null, block);
  }

  public BlockQuery(BlockState block) {
    this(null, block);
  }

  @Override
  public BlockState getBlock() {
    if (block == null) {
      block = world.getBlockAt(x, y, z).getState();
    }
    return block;
  }

  @Override
  public Location getLocation() {
    if (location == null) {
      location = new Location(world, x, y, z);
    }
    return location;
  }

  @Override
  public MaterialData getMaterial() {
    if (material == null) {
      material = getBlock().getMaterialData();
    }
    return material;
  }

  @Override
  public Match getMatch() {
    return PGM.get().getMatchManager().getMatch(world);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BlockQuery)) return false;
    BlockQuery query = (BlockQuery) o;
    return world.equals(query.world) && x == query.x && y == query.y && z == query.z;
  }

  @Override
  public int hashCode() {
    return Objects.hash(world, x, y, z);
  }
}
