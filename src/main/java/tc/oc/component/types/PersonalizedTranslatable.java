package tc.oc.component.types;

import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.command.CommandSender;
import tc.oc.component.Component;
import tc.oc.pgm.AllTranslations;
import tc.oc.util.components.Components;

/**
 * A {@link Component} that wraps around a {@link TranslatableComponent}
 *
 * <p>This component checks if the pattern provided exists in {@link AllTranslations}, if it does,
 * it returns a translated {@link PersonalizedText} component using the arguments. If it doesn't
 * exist, it uses a default {@link TranslatableComponent} as a fallback
 */
public class PersonalizedTranslatable extends Component {

  /**
   * Constructor
   *
   * @param original to clone
   */
  public PersonalizedTranslatable(TranslatableComponent original) {
    super(new TranslatableComponent(original));
  }

  /**
   * Constructor
   *
   * @param translate string to translate
   * @param with arguments
   */
  public PersonalizedTranslatable(String translate, Object... with) {
    super(new TranslatableComponent(translate, render(with)));
  }

  private static Object[] render(Object... toRender) {
    Object[] result = new Object[toRender.length];
    for (int i = 0; i < toRender.length; i++) {
      Object element = toRender[i];
      result[i] = element instanceof Component ? ((Component) element).render() : element;
    }
    return result;
  }

  @Override
  public BaseComponent render(CommandSender viewer) {
    TranslatableComponent component = getComponent();
    String pattern = AllTranslations.get().getPattern(component.getTranslate(), viewer);

    if (pattern != null) {
      // Found a TranslatableComponent with one of our keys
      List<Component> with = new ArrayList<>(component.getWith().size());
      for (BaseComponent extra : component.getWith()) with.add(new Component(extra));
      return new PersonalizedText(Components.format(pattern, with)).render(viewer);
    } else {
      // Fallback
      TranslatableComponent replacement = new TranslatableComponent(component);
      replacement.setWith(component.getWith());
      return replacement;
    }
  }

  public PersonalizedText getPersonalizedText() {
    return new PersonalizedText(render());
  }
}
