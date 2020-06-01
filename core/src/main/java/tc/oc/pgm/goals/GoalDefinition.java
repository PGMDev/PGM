package tc.oc.pgm.goals;

import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

/**
 * Definition of a goal/objective feature. Provides a name field, used to identify the goal to
 * players, and to generate a default ID. There is also a visibility flag. An invisible goal does
 * not appear in any scoreboards, chat messages, or anything else that would directly indicate its
 * existence.
 */
public abstract class GoalDefinition extends SelfIdentifyingFeatureDefinition {
  private final @Nullable Boolean required;
  private final boolean visible;
  private final String name;

  public GoalDefinition(
      @Nullable String id, String name, @Nullable Boolean required, boolean visible) {
    super(id);
    this.name = name;
    this.required = required;
    this.visible = visible;
  }

  @Override
  protected String getDefaultId() {
    return makeDefaultId() + "--" + makeId(this.name);
  }

  public String getName() {
    return this.name;
  }

  public String getColoredName() {
    return this.getName();
  }

  public Component getComponentName() {
    return TextComponent.of(getName());
  }

  public @Nullable Boolean isRequired() {
    return this.required;
  }

  public boolean isVisible() {
    return this.visible;
  }

  public Goal<? extends GoalDefinition> getGoal(Match match) {
    return (Goal<? extends GoalDefinition>) match.getFeatureContext().get(this.getId());
  }
}
