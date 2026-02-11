package tc.oc.pgm.platform.modern.modules.waypoints;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.jetbrains.annotations.Nullable;

public interface PGMWaypointTransmitter extends WaypointTransmitter {
  UUID uuid();

  @Nullable
  BlockPos position(ServerPlayer player);

  boolean shouldSee(ServerPlayer player);
}
