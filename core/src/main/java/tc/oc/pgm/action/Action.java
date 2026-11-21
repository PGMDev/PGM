package tc.oc.pgm.action;

/**
 * An action is a generic thing that can be triggered on an object. The object is commonly referred
 * to as the scope of the action. The most common and intuitive type of action is a kit, a kit is an
 * action scoped to a player, in other words, you can give the player a kit.
 *
 * @param <S> Scope of the action
 */
public interface Action<S> {

  Class<S> getScope();

  void trigger(S s, ActionContext context);

  void untrigger(S s, ActionContext context);

  default void trigger(S s) {
    trigger(s, ActionContext.EMPTY);
  }

  default void untrigger(S s) {
    untrigger(s, ActionContext.EMPTY);
  }
}
