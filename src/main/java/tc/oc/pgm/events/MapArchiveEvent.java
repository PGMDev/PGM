package tc.oc.pgm.events;

import java.io.File;
import javax.annotation.Nullable;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.match.Match;

public class MapArchiveEvent extends MatchEvent {
  private static HandlerList handlers = new HandlerList();

  protected File outputDirectory;

  public MapArchiveEvent(Match match, File outputDirectory) {
    super(match);
    this.outputDirectory = outputDirectory;
  }

  public @Nullable File getOutputDirectory() {
    return this.outputDirectory;
  }

  public void setOutputDirectory(@Nullable File dir) {
    this.outputDirectory = dir;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
