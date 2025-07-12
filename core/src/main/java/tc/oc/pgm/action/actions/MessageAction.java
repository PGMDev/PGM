package tc.oc.pgm.action.actions;

import static net.kyori.adventure.text.Component.text;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.replacements.Replacement;
import tc.oc.pgm.filters.Filterable;

public class MessageAction<T extends Filterable<?>> extends AbstractAction<T> {
  private static final Pattern PATTERN = Pattern.compile("\\{(.+?)}");
  private static final PlainTextComponentSerializer SERIALIZER =
      PlainTextComponentSerializer.plainText();

  private final Component text;
  private final Component actionbar;
  private final Title title;
  private final Map<String, Replacement> replacements;

  public MessageAction(
      Class<T> scope,
      @Nullable Component text,
      @Nullable Component actionbar,
      @Nullable Title title,
      @Nullable Map<String, Replacement> replacements) {
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

    BiFunction<MatchResult, TextComponent.Builder, ComponentLike> replacer = (match, original) -> {
      Replacement r = replacements.get(match.group(1));
      return r != null ? r.get(scope) : original;
    };

    component = component.replaceText(b -> b.match(PATTERN).replacement(replacer));
    component = replaceClickEvents(
        component,
        mr ->
            SERIALIZER.serialize(replacer.apply(mr, text().content(mr.group())).asComponent()));

    return component;
  }

  private Component replaceClickEvents(
      Component component, Function<MatchResult, String> replacer) {
    var click = component.clickEvent();
    if (click != null) {
      var matcher = PATTERN.matcher(click.value());
      var result = new StringBuilder();
      while (matcher.find()) matcher.appendReplacement(result, replacer.apply(matcher));
      matcher.appendTail(result);
      component = component.clickEvent(ClickEvent.clickEvent(click.action(), result.toString()));
    }
    var children = new ArrayList<>(component.children());
    children.replaceAll(child -> replaceClickEvents(child, replacer));
    return component.children(children);
  }

  private Title replace(Title title, T scope) {
    if (replacements == null) return title;
    return Title.title(
        replace(title.title(), scope), replace(title.subtitle(), scope), title.times());
  }
}
