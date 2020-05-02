package tc.oc.pgm.flag.state;

import java.time.Duration;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.PeriodFormats;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

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

    if (!Duration.ZERO.equals(respawnTime)) {
      // Respawn is delayed
      this.flag
          .getMatch()
          .sendMessage(
              new PersonalizedTranslatable(
                  "flag.willRespawn",
                  this.flag.getComponentName(),
                  new PersonalizedText(
                      PeriodFormats.briefNaturalApproximate(respawnTime),
                      net.md_5.bungee.api.ChatColor.AQUA)));
    }
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
      this.respawn(new PersonalizedTranslatable("flag.respawn", this.flag.getComponentName()));
    } else if (!this.wasCaptured) {
      // Flag was dropped
      this.respawn(new PersonalizedTranslatable("flag.return", this.flag.getComponentName()));
    } else if (this.wasDelayed) {
      // Flag was captured and respawn was delayed by a filter, so we announce that the flag has
      // respawned
      this.respawn(new PersonalizedTranslatable("flag.respawn", this.flag.getComponentName()));
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
  public String getStatusSymbol(Party viewer) {
    return Flag.RESPAWNING_SYMBOL;
  }

  @Override
  public ChatColor getStatusColor(Party viewer) {
    return ChatColor.GRAY;
  }
}
