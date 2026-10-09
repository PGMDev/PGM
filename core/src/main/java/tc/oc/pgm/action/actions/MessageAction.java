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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.action.replacements.Replacement;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.LocalFrame;

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
  public void trigger(T scope, ActionContext context) {
    var frame = context.frame();
    if (text != null) scope.sendMessage(replace(text, scope, frame));
    if (title != null) scope.showTitle(replace(title, scope, frame));
    if (actionbar != null) scope.sendActionBar(replace(actionbar, scope, frame));
  }

  private Component replace(Component component, T scope, @Nullable LocalFrame frame) {
    if (component == null || replacements == null) {
      return component;
    }

    BiFunction<MatchResult, TextComponent.Builder, ComponentLike> replacer = (match, original) -> {
      Replacement r = replacements.get(match.group(1));
      return r != null ? r.get(scope, frame) : original;
    };

    component = component.replaceText(b -> b.match(PATTERN).replacement(replacer));
    component = replaceClickEvents(
        component,
        mr ->
            SERIALIZER.serialize(replacer.apply(mr, text().content(mr.group())).asComponent()));

    return component;
  }

  private static @NonNull Component replaceClickEvents(
      Component component, Function<MatchResult, String> replacer) {
    var click = component.clickEvent();
    if (click != null
        && click.action() instanceof ClickEvent.Action.TextCarrier action
        && click.payload() instanceof ClickEvent.Payload.Text payload) {
      component = component.clickEvent(replaceEvent(action, payload, replacer));
    }
    var children = new ArrayList<>(component.children());
    children.replaceAll(child -> replaceClickEvents(child, replacer));
    return component.children(children);
  }

  private static @NonNull ClickEvent<ClickEvent.Payload.Text> replaceEvent(
      ClickEvent.Action<ClickEvent.Payload.Text> action,
      ClickEvent.Payload.Text payload,
      Function<MatchResult, String> replacer) {
    var matcher = PATTERN.matcher(payload.value());
    var result = new StringBuilder();
    while (matcher.find()) matcher.appendReplacement(result, replacer.apply(matcher));
    matcher.appendTail(result);
    var resultPayload = ClickEvent.Payload.string(result.toString());
    return ClickEvent.clickEvent(action, resultPayload);
  }

  private Title replace(Title title, T scope, @Nullable LocalFrame frame) {
    if (replacements == null) return title;
    return Title.title(
        replace(title.title(), scope, frame),
        replace(title.subtitle(), scope, frame),
        title.times());
  }
}
