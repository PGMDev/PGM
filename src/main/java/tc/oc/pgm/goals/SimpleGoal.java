package tc.oc.pgm.goals;

import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import tc.oc.component.Component;
import tc.oc.pgm.api.map.ProtoVersions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.util.logging.ClassLogger;

/** Basic {@link Goal} implementation with fields for the definition and match */
public abstract class SimpleGoal<T extends GoalDefinition> implements Goal<T> {

  public static final ChatColor COLOR_INCOMPLETE = ChatColor.RED;
  public static final ChatColor COLOR_COMPLETE = ChatColor.GREEN;

  public static final String SYMBOL_INCOMPLETE = "\u2715"; // ✕
  public static final String SYMBOL_COMPLETE = "\u2714"; // ✔

  protected final Logger logger;
  protected final T definition;
  protected final Match match;

  public SimpleGoal(T definition, Match match) {
    this.logger = ClassLogger.get(match.getLogger(), getClass());
    this.definition = definition;
    this.match = match;
  }

  @Override
  public Match getMatch() {
    return this.match;
  }

  @Override
  public T getDefinition() {
    return this.definition;
  }

  @Override
  public String getId() {
    return this.definition.getId();
  }

  @Override
  public String getName() {
    return this.definition.getName();
  }

  @Override
  public String getColoredName() {
    return this.definition.getColoredName();
  }

  @Override
  public Component getComponentName() {
    return this.definition.getComponentName();
  }

  @Override
  public Color getColor() {
    return getDyeColor().getColor();
  }

  @Override
  public DyeColor getDyeColor() {
    return DyeColor.WHITE;
  }

  @Override
  public boolean isVisible() {
    return this.definition.isVisible();
  }

  @Override
  public boolean isRequired() {
    Boolean required = getDefinition().isRequired();
    if (required != null) {
      return required;
    } else if (getMatch()
        .getMapContext()
        .getProto()
        .isNoOlderThan(ProtoVersions.GOAL_REQUIRED_OPTION)) {
      return true;
    } else {
      // Legacy behavior is to require no goals if score module is loaded
      return !getMatch().hasMatchModule(ScoreMatchModule.class);
    }
  }

  public ChatColor renderSidebarStatusColor(@Nullable Competitor competitor, Party viewer) {
    return isCompleted(competitor) ? COLOR_COMPLETE : COLOR_INCOMPLETE;
  }

  public String renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    return isCompleted(competitor) ? SYMBOL_COMPLETE : SYMBOL_INCOMPLETE;
  }

  public ChatColor renderSidebarLabelColor(@Nullable Competitor competitor, Party viewer) {
    return ChatColor.WHITE;
  }

  public String renderSidebarLabelText(@Nullable Competitor competitor, Party viewer) {
    return getName();
  }
}
