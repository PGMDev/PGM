package tc.oc.pgm.goals;

import javax.annotation.Nullable;
import org.bukkit.DyeColor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.server.BukkitUtils;

/** A goal with an owning team. Match-time companion to {@link OwnedGoal} */
public abstract class OwnedGoal<T extends OwnedGoalDefinition> extends SimpleGoal<T> {

  protected final Team owner;

  public OwnedGoal(T definition, Match match) {
    super(definition, match);
    this.owner = match.needModule(TeamMatchModule.class).getTeam(definition.getOwner());
  }

  public @Nullable Team getOwner() {
    return this.owner;
  }

  @Override
  public DyeColor getDyeColor() {
    return owner != null ? BukkitUtils.chatColorToDyeColor(owner.getColor()) : DyeColor.WHITE;
  }
}
