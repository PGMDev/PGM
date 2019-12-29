package tc.oc.component.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.experimental.Delegate;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import tc.oc.component.Component;

/**
 * A {@link Component} that is used to render a String.
 *
 * <p>This {@link Component} delegates and wraps around a {@link TextComponent} and uses it to
 * render text that can be applied formats
 */
public class PersonalizedText extends Component {

  @Delegate(types = TextComponent.class, excludes = BaseComponent.class)
  private TextComponent textComponent;

  public PersonalizedText() {
    super(new TextComponent());
    this.textComponent = getComponent();
  }

  /**
   * Constructor
   *
   * @param text of the component
   */
  public PersonalizedText(String text) {
    super(new TextComponent(text));
    this.textComponent = getComponent();
  }

  /**
   * Constructor
   *
   * @param extras extra {@link BaseComponent}s
   */
  public PersonalizedText(BaseComponent... extras) {
    this();
    this.setExtra(extras);
  }

  /**
   * Constructor
   *
   * @param extras extra {@link Component}s
   */
  public PersonalizedText(Component... extras) {
    this();
    List<BaseComponent> rendered = new ArrayList<>(extras.length);
    for (Component extra : extras) rendered.add(extra.render());
    this.setExtra(rendered);
  }

  /**
   * Constructor
   *
   * @param text of the component
   * @param formats applied to the component
   */
  public PersonalizedText(String text, ChatColor... formats) {
    this(text);
    this.add(formats);
  }

  /**
   * Constructor
   *
   * @param extra extra {@link Component}
   * @param formats applied to the component
   */
  public PersonalizedText(Component extra, ChatColor... formats) {
    this(Collections.singletonList(extra));
    this.add(formats);
  }

  /**
   * Constructor
   *
   * @param formats applied to the component
   */
  public PersonalizedText(ChatColor... formats) {
    this();
    this.add(formats);
  }

  /**
   * Constructor
   *
   * @param personalizedText to clone
   */
  public PersonalizedText(PersonalizedText personalizedText) {
    this(personalizedText.getText());
    this.copyFormatting(personalizedText.render());
    this.textComponent = personalizedText.textComponent;
  }

  /**
   * Constructor
   *
   * @param extraCapacity maximum extra capacity
   */
  public PersonalizedText(int extraCapacity) {
    this();
    setExtra(new ArrayList<>(extraCapacity));
  }

  /**
   * Constructor
   *
   * @param extras {@link List} of {@link Component} extras
   */
  public PersonalizedText(List<Component> extras) {
    this("", extras);
  }

  /**
   * Constructor
   *
   * @param text of the component
   * @param extras {@link List} of {@link Component} extras
   */
  public PersonalizedText(String text, List<Component> extras) {
    this(text);
    List<BaseComponent> rendered = new ArrayList<>(extras.size());
    for (Component extra : extras) rendered.add(extra.render());
    this.setExtra(rendered);
  }

  public Component text(String text) {
    this.setText(text);
    return this;
  }

  @Override
  public BaseComponent duplicate() {
    return new PersonalizedText(this).render();
  }

  @Override
  public BaseComponent duplicateWithoutFormatting() {
    return new PersonalizedText(this.getText()).render();
  }
}
