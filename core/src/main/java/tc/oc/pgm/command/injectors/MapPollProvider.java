package tc.oc.pgm.command.injectors;

import static tc.oc.pgm.util.text.TextException.exception;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.injection.ParameterInjector;
import org.incendo.cloud.util.annotation.AnnotationAccessor;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.rotation.pools.VotingPool;
import tc.oc.pgm.rotation.vote.MapPoll;

public final class MapPollProvider implements ParameterInjector<CommandSender, MapPoll> {

  @Override
  public MapPoll create(
      @NotNull CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    MapOrder mapOrder = PGM.get().getMapOrder();
    if (!(mapOrder instanceof MapPoolManager))
      throw new CommandExecutionException(exception("pool.mapPoolsDisabled"));

    MapPool pool = ((MapPoolManager) mapOrder).getActiveMapPool();
    MapPoll poll = pool instanceof VotingPool ? ((VotingPool) pool).getCurrentPoll() : null;

    if (poll != null) return poll;
    throw new CommandExecutionException(exception("vote.noVote"));
  }
}
