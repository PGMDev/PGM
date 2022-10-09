package tc.oc.pgm.goals;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.teams.TeamFactory;

/**
 * Definition of a goal that is "owned" by a particular team. The ramifications of ownership depend
 * entirely on the type of goal. Some goals are pursued by their owner, some are defended by their
 * owner. The only thing the base class does with the owner is store it and use it as part of the
 * default ID.
 */
public abstract class OwnedGoalDefinition extends GoalDefinition {
  private final TeamFactory owner;

  public OwnedGoalDefinition(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      ShowOptions showOptions,
      @Nullable TeamFactory owner) {
    super(id, name, required, showOptions);
    this.owner = owner;
  }

  @Override
  protected String getDefaultId() {
    return this.getOwner().getId() + super.getDefaultId();
  }

  public TeamFactory getOwner() {
    return this.owner;
  }
}
