package tc.oc.pgm.action;

import tc.oc.pgm.api.filter.query.Query;

/**
 * An action is a generic thing that can be triggered on an object. The object is commonly referred
 * to as the scope of the action. The most common and intuitive type of action is a kit, a kit is an
 * action scoped to a player, in other words, you can give the player a kit.
 *
 * @param <S> Scope of the action
 */
public interface Action<S> {

  Class<S> getScope();

  void trigger(S s);

  /**
   * Trigger the action with a specific query, this should be used by actions that act on a filter,
   * but otherwise can be ingored.
   *
   * @param s the scope to act on
   * @param event the query to use
   */
  default void trigger(S s, Query event) {
    trigger(s);
  }

  void untrigger(S s);
}
