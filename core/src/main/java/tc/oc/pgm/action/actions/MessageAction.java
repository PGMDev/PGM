package tc.oc.pgm.action.actions;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.Audience;

public class MessageAction<T extends Audience> extends AbstractAction<T> {
  private static final Pattern REPLACEMENT_PATTERN = Pattern.compile("\\{(.+?)}");

  private final Component text;
  private final Component actionbar;
  private final Title title;
  private final Map<String, Replacement<T>> replacements;

  public MessageAction(
      Class<T> scope,
      @Nullable Component text,
      @Nullable Component actionbar,
      @Nullable Title title,
      @Nullable Map<String, Replacement<T>> replacements) {
    super(scope);
    this.text = text;
    this.actionbar = actionbar;
    this.title = title;
    this.replacements = replacements;
  }

  @Override
  public void trigger(T scope) {
    if (text != null) scope.sendMessage(replace(text, scope));
    if (title != null) scope.showTitle(replace(title, scope));
    if (actionbar != null) scope.sendActionBar(replace(actionbar, scope));
  }

  private Component replace(Component component, T scope) {
    if (component == null || replacements == null) {
      return component;
    }

    return component.replaceText(
        TextReplacementConfig.builder()
            .match(REPLACEMENT_PATTERN)
            .replacement(
                (match, original) -> {
                  Replacement<T> r = replacements.get(match.group(1));
                  return r != null ? r.apply(scope) : original;
                })
            .build());
  }

  private Title replace(Title title, T scope) {
    if (replacements == null) return title;
    return Title.title(
        replace(title.title(), scope), replace(title.subtitle(), scope), title.times());
  }

  public interface Replacement<T extends Audience> extends Function<T, ComponentLike> {}
}
