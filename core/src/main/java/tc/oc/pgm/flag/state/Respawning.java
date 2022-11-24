package tc.oc.pgm.flag.state;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;

/**
 * State of a flag while it is waiting to respawn at a {@link Post} after being {@link Captured}.
 * This phase can be delayed by respawn-time or respawn-speed.
 */
public class Respawning extends Spawned implements Returning {

  protected final @Nullable Location respawnFrom;
  protected final Location respawnTo;
  protected final Duration respawnTime;
  protected final boolean wasCaptured;
  protected final boolean wasDelayed;

  protected Respawning(
      Flag flag,
      Post post,
      @Nullable Location respawnFrom,
      boolean wasCaptured,
      boolean wasDelayed) {
    super(flag, post);
    this.respawnFrom = respawnFrom;
    this.respawnTo = this.flag.getReturnPoint(this.post);
    this.respawnTime =
        this.post.getRespawnTime(
            this.respawnFrom == null ? 0 : this.respawnFrom.distance(this.respawnTo));
    this.wasCaptured = wasCaptured;
    this.wasDelayed = wasDelayed;
  }

  @Override
  protected Duration getDuration() {
    return respawnTime;
  }

  @Override
  public void enterState() {
    super.enterState();

    if (Duration.ZERO.equals(respawnTime)) return;
    // Respawn is delayed
    String postName = this.post.getPostName();

    TranslatableComponent timeComponent = duration(respawnTime, NamedTextColor.AQUA);
    Component message =
        postName != null
            ? translatable(
                "flag.willRespawn.named",
                this.flag.getComponentName(),
                text(postName, NamedTextColor.AQUA),
                timeComponent)
            : translatable("flag.willRespawn", this.flag.getComponentName(), timeComponent);
    this.flag.getMatch().sendMessage(message);
  }

  protected void respawn(@Nullable Component message) {
    if (message != null) {
      this.flag.playStatusSound(Flag.RETURN_SOUND_OWN, Flag.RETURN_SOUND);
      this.flag.getMatch().sendMessage(message);
    }

    this.flag.transition(new Returned(this.flag, this.post, this.respawnTo));
  }

  @Override
  protected void tickSeconds(long seconds) {
    super.tickSeconds(seconds);
    this.flag.getMatch().callEvent(new GoalStatusChangeEvent(this.flag.getMatch(), this.flag));
  }

  @Override
  protected void finishCountdown() {
    super.finishCountdown();

    if (!Duration.ZERO.equals(respawnTime)) {
      this.respawn(translatable("flag.respawn", this.flag.getComponentName()));
    } else if (!this.wasCaptured) {
      // Flag was dropped
      this.respawn(translatable("flag.return", this.flag.getComponentName()));
    } else if (this.wasDelayed) {
      // Flag was captured and respawn was delayed by a filter, so we announce that the flag has
      // respawned
      this.respawn(translatable("flag.respawn", this.flag.getComponentName()));
    }
  }

  @Override
  public Location getLocation() {
    return this.respawnTo;
  }

  @Override
  public boolean isRecoverable() {
    return false;
  }

  @Override
  public Component getStatusSymbol(Party viewer) {
    return Flag.RESPAWNING_SYMBOL;
  }

  @Override
  public TextColor getStatusColor(Party viewer) {
    return NamedTextColor.GRAY;
  }
}
