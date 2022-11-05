package tc.oc.pgm.command.injectors;

import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.AnnotationAccessor;
import cloud.commandframework.annotations.injection.ParameterInjector;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.CommandExecutionException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.rotation.MapPoolManager;

public final class MapPoolManagerProvider
    implements ParameterInjector<CommandSender, MapPoolManager> {

  @Override
  public MapPoolManager create(
      @NotNull CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    MapOrder mapOrder = PGM.get().getMapOrder();
    if (mapOrder instanceof MapPoolManager) return (MapPoolManager) mapOrder;
    throw new CommandExecutionException(exception("pool.mapPoolsDisabled"));
  }
}
