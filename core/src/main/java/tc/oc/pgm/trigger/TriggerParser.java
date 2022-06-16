package tc.oc.pgm.trigger;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Method;
import java.util.Map;
import javax.annotation.Nonnull;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.dynamic.Filterable;
import tc.oc.pgm.trigger.modifier.TriggerModifiers;
import tc.oc.pgm.trigger.triggers.ChatMessageTrigger;
import tc.oc.pgm.trigger.triggers.KitTrigger;
import tc.oc.pgm.trigger.triggers.TriggerNode;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class TriggerParser {

  private final MapFactory factory;
  private final FilterParser filters;
  private final Map<String, Method> methodParsers;

  public TriggerParser(MapFactory factory) {
    this.factory = factory;
    this.filters = factory.getFilters();
    this.methodParsers = MethodParsers.getMethodParsersForClass(getClass());
  }

  public <B extends Filterable<?>> Trigger<? super B> parse(Element el, @Nullable Class<B> bound)
      throws InvalidXMLException {
    String id = FeatureDefinitionContext.parseId(el);

    if (id != null && maybeReference(el)) {
      return parseReference(new Node(el), id, bound);
    }

    TriggerDefinition<? super B> result =
        "trigger".equals(el.getName()) && bound != null
            ? parseDefinitionBounded(el, bound)
            : parseDynamic(el);

    Class<?> childBound = result.getTriggerType();
    if (bound != null && !childBound.isAssignableFrom(bound)) {
      throw new InvalidXMLException(
          "Wrong trigger target, expected "
              + bound.getSimpleName()
              + " rather than "
              + childBound.getSimpleName(),
          new Node(el));
    }

    factory.getFeatures().addFeature(el, result);
    return result;
  }

  public boolean isTrigger(Element el) {
    return getParserFor(el) != null;
  }

  private boolean maybeReference(Element el) {
    return "trigger".equals(el.getName()) && el.getChildren().isEmpty();
  }

  public <B> Trigger<? super B> parseReference(Node node, Class<B> bound)
      throws InvalidXMLException {
    return parseReference(node, node.getValue(), bound);
  }

  public <B> Trigger<? super B> parseReference(Node node, String id, Class<B> bound)
      throws InvalidXMLException {
    return factory
        .getFeatures()
        .addReference(new XMLTriggerReference<>(factory.getFeatures(), node, id, bound));
  }

  private <B extends Filterable<?>> TriggerDefinition<? super B> parseDefinitionBounded(
      Element el, @Nonnull Class<B> bound) throws InvalidXMLException {
    ImmutableList.Builder<Trigger<? super B>> builder = ImmutableList.builder();
    for (Element child : el.getChildren()) {
      builder.add(parse(child, bound));
    }

    return new TriggerNode<>(builder.build(), bound);
  }

  protected Method getParserFor(Element el) {
    return methodParsers.get(el.getName().toLowerCase());
  }

  private <T> TriggerDefinition<T> parseDynamic(Element el) throws InvalidXMLException {
    Method parser = getParserFor(el);
    if (parser != null) {
      try {
        //noinspection unchecked
        return (TriggerDefinition<T>) parser.invoke(this, el);
      } catch (Exception e) {
        throw InvalidXMLException.coerce(e, new Node(el));
      }
    } else {
      throw new InvalidXMLException("Unknown trigger type: " + el.getName(), el);
    }
  }

  public <T extends Filterable<?>> TriggerRule<T> parseRule(Element el) throws InvalidXMLException {
    Class<T> cls = parseFilterable(Node.fromRequiredAttr(el, "scope"));
    return new TriggerRule<>(
        cls,
        filters.parseReference(Node.fromRequiredAttr(el, "filter")),
        parseReference(Node.fromRequiredAttr(el, "trigger"), cls));
  }

  @SuppressWarnings("unchecked")
  private <T extends Filterable<?>> Class<T> parseFilterable(Node node) throws InvalidXMLException {
    switch (node.getValueNormalize()) {
      case "player":
        return (Class<T>) MatchPlayer.class;
      case "team":
        return (Class<T>) Party.class;
      case "match":
        return (Class<T>) Match.class;
      default:
        throw new InvalidXMLException("Unknown scope, must be one of: player, team, match", node);
    }
  }

  @MethodParser("trigger")
  public TriggerDefinition<?> parseDefinition(Element el) throws InvalidXMLException {
    return parseDefinitionBounded(el, parseFilterable(Node.fromRequiredAttr(el, "scope")));
  }

  @MethodParser("kit")
  public KitTrigger parseKitTrigger(Element el) throws InvalidXMLException {
    return new KitTrigger(factory.getKits().parseKitProperty(el, "kit"));
  }

  @MethodParser("switch-scope")
  public <O extends Filterable<?>, I extends Filterable<?>> Trigger<? super O> parseSwitchScope(
      Element el) throws InvalidXMLException {
    Class<O> outer = parseFilterable(Node.fromRequiredAttr(el, "outer"));
    Class<I> inner = parseFilterable(Node.fromRequiredAttr(el, "inner"));

    TriggerDefinition<? super I> child = parseDefinitionBounded(el, inner);

    Trigger<? super O> result = TriggerModifiers.modify(child, outer, inner);
    if (result == null) {
      throw new InvalidXMLException(
          "Could not convert from " + outer.getSimpleName() + "to" + inner.getSimpleName(), el);
    }
    return result;
  }

  @MethodParser("message")
  public ChatMessageTrigger parseChatMessage(Element el) throws InvalidXMLException {
    return new ChatMessageTrigger(XMLUtils.parseFormattedText(Node.fromRequiredAttr(el, "text")));
  }
}
