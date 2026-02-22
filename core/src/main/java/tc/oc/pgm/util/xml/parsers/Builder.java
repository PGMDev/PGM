package tc.oc.pgm.util.xml.parsers;

import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.function.ThrowingSupplier;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public abstract class Builder<T, B extends Builder<T, B>> {
  protected final @Nullable Element el;
  protected final String[] prop;

  protected List<Validator<T>> validators;
  protected boolean attr;
  protected boolean child;
  protected boolean self;

  public Builder(@Nullable Element el, String... prop) {
    this.el = el;
    this.prop = prop;
    this.attr = prop.length > 0;
    this.child = prop.length > 0;
    this.self = prop.length == 0;
  }

  /** Make it so this parser will only run on attributes, ignoring child elements */
  public B attr() {
    assert prop.length != 0;
    attr = true;
    child = false;
    return getThis();
  }

  /** Make it so this parser will only run on child elements, ignoring attributes */
  public B child() {
    assert prop.length != 0;
    attr = false;
    child = true;
    return getThis();
  }

  public B validate(Validator<T> validator) {
    if (validators == null) validators = new ArrayList<>();
    validators.add(validator);
    return getThis();
  }

  public T required() throws InvalidXMLException {
    return handleParse(getNode(true));
  }

  public T optional(T defaultValue) throws InvalidXMLException {
    Node node = getNode(false);
    return node == null ? defaultValue : handleParse(node);
  }

  public T optional(ThrowingSupplier<T, InvalidXMLException> defaultValue)
      throws InvalidXMLException {
    Node node = getNode(false);
    return node == null ? defaultValue.get() : handleParse(node);
  }

  public Optional<T> optional() throws InvalidXMLException {
    Node node = getNode(false);
    return node == null ? Optional.empty() : Optional.of(handleParse(node));
  }

  public T orNull() throws InvalidXMLException {
    return optional((T) null);
  }

  public T orSelf() throws InvalidXMLException {
    attr = true;
    child = false;
    self = true;
    return handleParse(getNode(true));
  }

  protected Node getNode(boolean required) throws InvalidXMLException {
    return Node.from(attr, child, self, required, el, prop);
  }

  T handleParse(Node node) throws InvalidXMLException {
    T value = parse(node);
    if (validators != null) {
      for (Validator<T> validator : validators) {
        validator.validate(value, node);
      }
    }
    return value;
  }

  protected abstract T parse(Node node) throws InvalidXMLException;

  protected abstract B getThis();

  public interface Validator<T> {
    void validate(T t, Node node) throws InvalidXMLException;
  }

  public abstract static class Generic<T> extends Builder<T, Generic<T>> {
    public Generic(@Nullable Element el, String... prop) {
      super(el, prop);
    }

    @Override
    protected Generic<T> getThis() {
      return this;
    }
  }

  /**
   * Extension methods that can be adopted by any builder that uses a comparable value
   *
   * @param <T> type being parsed
   * @param <B> builder type
   */
  public interface WithRange<T extends Comparable<T>, B extends Builder<T, B>> {
    B validate(Validator<T> validation);

    default B between(Range<T> range) throws InvalidXMLException {
      return validate((value, node) -> {
        if (!range.contains(value))
          throw new InvalidXMLException(value + " is not in the range " + range, node);
      });
    }

    default B min(T min) throws InvalidXMLException {
      return between(Range.atLeast(min));
    }

    default B max(T max) throws InvalidXMLException {
      return between(Range.atMost(max));
    }
  }
}
