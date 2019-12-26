package tc.oc.component.types;

import java.util.List;
import java.util.stream.Collectors;
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
    for (int i = 0; i < toRender.length; i++) {
      Object element = toRender[i];
      if (element instanceof Component) toRender[i] = ((Component) element).render();
    }
    return toRender;
  }

  @Override
  public BaseComponent render(CommandSender viewer) {
    TranslatableComponent component = getComponent();
    String pattern = AllTranslations.get().getPattern(component.getTranslate(), viewer);

    if (pattern != null) {
      // Found a TranslatableComponent with one of our keys
      List<Component> with =
          component.getWith().stream().map(Component::new).collect(Collectors.toList());
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
