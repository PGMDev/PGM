package tc.oc.pgm.flag.state;

import java.time.Duration;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.util.text.PeriodFormats;

/**
 * State of a flag while it is waiting to respawn at a {@link Post} after being {@link Captured}.
 * This phase can be delayed by respawn-time or respawn-speed.
 */
public class Respawning extends Spawned implements Returning {

  protected final @Nullable Location respawnFrom;
  protected final Location respawnTo;
  protected final Post respawnToPost;
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
    respawnToPost = this.flag.getReturnPost(this.post);
    this.respawnTo = this.respawnToPost.getReturnPoint(this.flag, this.flag.getBannerYawProvider());
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
      String postName = this.respawnToPost.getPostName();
      Component timeComponent =
          PeriodFormats.briefNaturalApproximate(respawnTime).color(TextColor.AQUA);

      if (postName != null) {
        this.flag
            .getMatch()
            .sendMessage(
                TranslatableComponent.of(
                    "flag.willRespawn.named",
                    this.flag.getComponentName(),
                    TextComponent.of(postName, TextColor.AQUA),
                    timeComponent));
      } else {
        this.flag
            .getMatch()
            .sendMessage(
                TranslatableComponent.of(
                    "flag.willRespawn", this.flag.getComponentName(), timeComponent));
      }
    }
  }

  protected void respawn(@Nullable Component message) {
    if (message != null) {
      this.flag.playStatusSound(Flag.RETURN_SOUND_OWN, Flag.RETURN_SOUND);
      this.flag.getMatch().sendMessage(message);
    }

    this.flag.transition(new Returned(this.flag, this.respawnToPost, this.respawnTo));
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
      this.respawn(TranslatableComponent.of("flag.respawn", this.flag.getComponentName()));
    } else if (!this.wasCaptured) {
      // Flag was dropped
      this.respawn(TranslatableComponent.of("flag.return", this.flag.getComponentName()));
    } else if (this.wasDelayed) {
      // Flag was captured and respawn was delayed by a filter, so we announce that the flag has
      // respawned
      this.respawn(TranslatableComponent.of("flag.respawn", this.flag.getComponentName()));
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
