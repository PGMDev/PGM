package tc.oc.pgm.goals;

import org.bukkit.DyeColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

/** A goal with an owning team. Match-time companion to {@link OwnedGoal} */
public abstract class OwnedGoal<T extends OwnedGoalDefinition> extends SimpleGoal<T> {

  protected final Team owner;

  public OwnedGoal(T definition, Match match) {
    super(definition, match);
    this.owner =
        definition.getOwner() == null
            ? null
            : match.needModule(TeamMatchModule.class).getTeam(definition.getOwner());
  }

  public @Nullable Team getOwner() {
    return this.owner;
  }

  @Override
  public DyeColor getDyeColor() {
    return owner != null ? owner.getDyeColor() : DyeColor.WHITE;
  }
}
