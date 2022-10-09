package tc.oc.pgm.goals;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.base.Splitter;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.Nullable;
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
  private final ShowOptions showOptions;
  private final String name;
  private final Component component;

  public GoalDefinition(
      @Nullable String id, String name, @Nullable Boolean required, ShowOptions showOptions) {
    super(id);
    this.name = name;
    this.component = translateName(name);
    this.required = required;
    this.showOptions = showOptions;
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
    return this.component;
  }

  public @Nullable Boolean isRequired() {
    return this.required;
  }

  public boolean hasShowOption(ShowOption option) {
    return this.getShowOptions().hasOption(option);
  }

  protected ShowOptions getShowOptions() {
    return this.showOptions;
  }

  public Goal<? extends GoalDefinition> getGoal(Match match) {
    return (Goal<? extends GoalDefinition>) match.getFeatureContext().get(this.getId());
  }

  // See "objective.name.monument" for examples
  private static final Pattern OBJECTIVE_PATTERN =
      Pattern.compile(
          "Monument|Core|Wool|Flag|Antenna|Base|Ship|Orb|Tower|Pillar|Inhibitor|Reactor|Engine");

  // See "misc.top" for examples
  private static final Pattern DESCRIPTOR_PATTERN =
      Pattern.compile(
          "Top|Bottom|Front|Back|Rear|Left|Right|Center|Mid|North|South|East|West|White|Orange|Magenta|Yellow|Lime|Pink|Gray|Cyan|Purple|Blue|Brown|Green|Red|Black|Light");

  // TODO: Support languages where words are ordered right-to-left
  private static Component translateName(final String name) {
    final TextComponent.Builder text = text();
    int results = 0;
    for (final String section : Splitter.on(' ').omitEmptyStrings().trimResults().split(name)) {
      if (results > 0) {
        text.append(space());
      }
      if (OBJECTIVE_PATTERN.matcher(section).matches()) {
        text.append(translatable("objective.name." + section.toLowerCase()));
      } else if (DESCRIPTOR_PATTERN.matcher(section).matches()) {
        text.append(translatable("misc." + section.toLowerCase()));
      } else {
        text.append(text(section));
      }
      results++;
    }
    return text.build();
  }
}
