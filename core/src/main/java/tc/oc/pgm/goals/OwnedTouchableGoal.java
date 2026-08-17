package tc.oc.pgm.goals;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.translatable;

import java.util.Collections;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.named.NameStyle;

/**
 * A {@link TouchableGoal} that always belongs to exactly one {@link Team} and is defended by that
 * team, i.e. cores and destroyables.
 */
public abstract class OwnedTouchableGoal<T extends ProximityGoalDefinition>
    extends TouchableGoal<T> {

  private Iterable<Location> proximityLocations;

  public OwnedTouchableGoal(T definition, Match match) {
    super(definition, match);
  }

  /** The region whose center players are measured against for proximity. */
  protected abstract Region getProximityRegion();

  @Override
  public @NonNull Team getOwner() {
    Team owner = super.getOwner();
    if (owner == null) {
      throw new IllegalStateException("goal " + this.getId() + " has no owner");
    }
    return owner;
  }

  @Override
  public boolean getDeferTouches() {
    return true;
  }

  @Override
  public Component getTouchMessage(@Nullable ParticipantState toucher, boolean self) {
    String key = "destroyable.touch.owned";
    Component toucherName = empty();

    if (toucher != null) {
      if (self) {
        key = "destroyable.touch.owned.you";
      } else {
        key = "destroyable.touch.owned.player";
        toucherName = toucher.getName(NameStyle.COLOR);
      }
    }

    return translatable(
        key, toucherName, this.getComponentName(), this.getOwner().getName());
  }

  @Override
  public Iterable<Location> getProximityLocations(ParticipantState player) {
    if (this.proximityLocations == null) {
      this.proximityLocations = Collections.singleton(getProximityRegion()
          .getBounds()
          .getCenterPoint()
          .toLocation(getMatch().getWorld()));
    }
    return this.proximityLocations;
  }
}
