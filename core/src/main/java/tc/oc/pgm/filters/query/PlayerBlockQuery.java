package tc.oc.pgm.filters.query;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.query.BlockQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.util.material.MaterialData;

public class PlayerBlockQuery extends PlayerStateQuery implements BlockQuery {

  private final BlockState block;
  private @Nullable MaterialData material;

  public PlayerBlockQuery(@Nullable Event event, MatchPlayer player, BlockState block) {
    this(event, player.getParticipantState(), block);
  }

  public PlayerBlockQuery(@Nullable Event event, ParticipantState player, BlockState block) {
    super(event, player);
    this.block = assertNotNull(block);
  }

  public PlayerBlockQuery withMaterial(MaterialData material) {
    this.material = material;
    return this;
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
    if (material == null) {
      material = MaterialData.block(block);
    }
    return material;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlayerBlockQuery query)) return false;
    if (!super.equals(o)) return false;
    return block.equals(query.block) && getMaterial() == query.getMaterial();
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), block, getMaterial());
  }
}
