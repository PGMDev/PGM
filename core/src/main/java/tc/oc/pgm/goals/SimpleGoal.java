package tc.oc.pgm.goals;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.text;

import java.util.logging.Logger;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.util.ClassLogger;

/** Basic {@link Goal} implementation with fields for the definition and match */
public abstract class SimpleGoal<T extends GoalDefinition> implements Goal<T> {

  public static final TextColor COLOR_INCOMPLETE = NamedTextColor.RED;
  public static final TextColor COLOR_COMPLETE = NamedTextColor.GREEN;

  public static final Component SYMBOL_INCOMPLETE = text("\u2715"); // ✕
  public static final Component SYMBOL_COMPLETE = text("\u2714"); // ✔

  protected static final Sound GOOD_SOUND =
      sound(key("portal.travel"), Sound.Source.MASTER, 0.7f, 2f);
  protected static final Sound BAD_SOUND =
      sound(key("mob.blaze.death"), Sound.Source.MASTER, 0.8f, 0.8f);

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
  public Sound getCompletionSound(boolean isGood) {
    return isGood ? GOOD_SOUND : BAD_SOUND;
  }

  @Override
  public boolean hasShowOption(ShowOption flag) {
    return this.definition.hasShowOption(flag);
  }

  public Filter getScoreboardFilter() {
    return this.definition.getShowOptions().getScoreboardFilter();
  }

  @Override
  public boolean isRequired() {
    Boolean required = getDefinition().isRequired();
    if (required != null) {
      return required;
    } else if (getMatch().getMap().getProto().isNoOlderThan(MapProtos.GOAL_REQUIRED_OPTION)) {
      return true;
    } else {
      // Legacy behavior is to require no goals if score module is loaded
      return !getMatch().hasModule(ScoreMatchModule.class);
    }
  }

  public TextColor renderSidebarStatusColor(@Nullable Competitor competitor, Party viewer) {
    return isCompleted() ? COLOR_COMPLETE : COLOR_INCOMPLETE;
  }

  public Component renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    return isCompleted() ? SYMBOL_COMPLETE : SYMBOL_INCOMPLETE;
  }

  public TextColor renderSidebarLabelColor(@Nullable Competitor competitor, Party viewer) {
    return NamedTextColor.WHITE;
  }

  public Component renderSidebarLabelText(@Nullable Competitor competitor, Party viewer) {
    return getComponentName();
  }
}
