package tc.oc.pgm.prefix;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PrefixChangeEvent extends Event {

  private final UUID uuid;
  private final String oldPrefix;
  private final String newPrefix;

  public PrefixChangeEvent(UUID uuid, String oldPrefix, String newPrefix) {
    this.uuid = uuid;
    this.oldPrefix = oldPrefix;
    this.newPrefix = newPrefix;
  }

  public UUID getUUID() {
    return uuid;
  }

  @Nullable
  public String getOldPrefix() {
    return oldPrefix;
  }

  public String getNewPrefix() {
    return newPrefix;
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
