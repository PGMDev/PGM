package tc.oc.pgm.filters.query;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.query.BlockQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;

public class PlayerBlockQuery extends PlayerStateQuery implements BlockQuery {

  private final BlockState block;

  public PlayerBlockQuery(@Nullable Event event, MatchPlayer player, BlockState block) {
    this(event, player.getParticipantState(), block);
  }

  public PlayerBlockQuery(@Nullable Event event, ParticipantState player, BlockState block) {
    super(event, player);
    this.block = assertNotNull(block);
  }

  @Override
  public BlockState getBlock() {
    return block;
  }

  @Override
  public Location getLocation() {
    return block.getLocation();
  }

  @Override
  public MaterialData getMaterial() {
    return block.getData();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlayerBlockQuery)) return false;
    if (!super.equals(o)) return false;
    PlayerBlockQuery query = (PlayerBlockQuery) o;
    if (!block.equals(query.block)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + block.hashCode();
    return result;
  }
}
