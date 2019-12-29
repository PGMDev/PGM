package tc.oc.component;

import app.ashcon.sportpaper.api.text.PersonalizedComponent;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import lombok.experimental.Delegate;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import tc.oc.component.types.PersonalizedText;
import tc.oc.util.components.Components;

/**
 * The main component used across all translations for PGM.
 *
 * <p>This component delegates bungee's {@link BaseComponent} and acts as a wrapper to access it.
 * This class should only be used for cloning and creating new components.
 */
public class Component implements PersonalizedComponent {

  @Delegate(types = BaseComponent.class)
  private BaseComponent baseComponent;

  /**
   * Constructor
   *
   * @param baseComponent base component to render
   */
  public Component(BaseComponent baseComponent) {
    this.baseComponent = baseComponent;
  }

  @Override
  public BaseComponent render(CommandSender commandSender) {
    return baseComponent;
  }

  /**
   * Renders the component using the {@link Bukkit#getConsoleSender}
   *
   * @return {@link BaseComponent} rendered
   */
  public BaseComponent render() {
    return render(Bukkit.getConsoleSender());
  }

  /**
   * Returns the instance of the base component. Refrain from using this method for the actual
   * rendering of the component as it will not render and only return the content of the provided
   * {@link T} component
   *
   * @param <T> type of used by the component
   * @return {@link T} component
   */
  @SuppressWarnings("unchecked")
  public <T extends BaseComponent> T getComponent() {
    return (T) baseComponent;
  }

  public Component add(ChatColor... formats) {
    Components.addFormats(this, formats);
    return this;
  }

  public Component remove(ChatColor... formats) {
    Components.removeFormats(this, formats);
    return this;
  }

  public Component color(@Nullable ChatColor color) {
    this.setColor(color);
    return this;
  }

  public Component bold(@Nullable Boolean yes) {
    this.setBold(yes);
    return this;
  }

  public Component italic(@Nullable Boolean yes) {
    this.setItalic(yes);
    return this;
  }

  public Component underlined(@Nullable Boolean yes) {
    this.setUnderlined(yes);
    return this;
  }

  public Component strikethrough(@Nullable Boolean yes) {
    this.setStrikethrough(yes);
    return this;
  }

  public Component obfuscated(@Nullable Boolean yes) {
    this.setObfuscated(yes);
    return this;
  }

  public Component clickEvent(@Nullable ClickEvent event) {
    this.setClickEvent(event);
    return this;
  }

  public Component clickEvent(ClickEvent.Action action, String value) {
    this.setClickEvent(new ClickEvent(action, value));
    return this;
  }

  public Component hoverEvent(@Nullable HoverEvent event) {
    this.setHoverEvent(event);
    return this;
  }

  public Component hoverEvent(HoverEvent.Action action, BaseComponent... values) {
    this.setHoverEvent(new HoverEvent(action, values));
    return this;
  }

  public Component extra(Component... extras) {
    return this.extra(Arrays.asList(extras));
  }

  public Component extra(String extra) {
    return this.extra(new PersonalizedText(extra));
  }

  public Component extra(Collection<Component> extras) {
    List<BaseComponent> components = new ArrayList<>(extras.size());
    for (Component extra : extras) components.add(extra.render());
    if (this.getExtra() == null) {
      this.setExtra(components);
    } else {
      this.setExtra(Lists.newArrayList(Iterables.concat(this.getExtra(), components)));
    }
    return this;
  }
}
