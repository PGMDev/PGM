package tc.oc.pgm.trigger;

/**
 * A trigger is a generic action that can occur on an object. The object is commonly referred to as
 * the scope of the trigger. The most common and intuitive type of trigger is a kit, a kit is a
 * trigger scoped to a player, in other words, you can give the player a kit.
 *
 * @param <S> Scope of the trigger
 */
public interface Trigger<S> {

  Class<S> getScope();

  void trigger(S s);

  void untrigger(S s);
}
