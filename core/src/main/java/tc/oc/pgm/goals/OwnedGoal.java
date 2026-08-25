package tc.oc.pgm.goals;

import org.bukkit.DyeColor;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

/** A goal with an owning team. Match-time companion to {@link OwnedGoal} */
public abstract class OwnedGoal<T extends OwnedGoalDefinition> extends SimpleGoal<T> {

  protected final @Nullable Team owner;

  public OwnedGoal(T definition, Match match) {
    super(definition, match);
    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    this.owner =
        (definition.getOwner() != null && tmm != null) ? tmm.getTeam(definition.getOwner()) : null;
  }

  public @Nullable Team getOwner() {
    return this.owner;
  }

  @Override
  public DyeColor getDyeColor() {
    return owner != null ? owner.getDyeColor() : DyeColor.WHITE;
  }
}
