package tc.oc.pgm.trigger.modifier;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.util.function.Function;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.trigger.Trigger;
import tc.oc.pgm.util.xml.InvalidXMLException;

public interface TriggerModifiers {

  @SuppressWarnings("unchecked")
  Table<Class<?>, Class<?>, Function<Trigger<?>, Trigger<?>>> CONVERSIONS =
      ImmutableTable.<Class<?>, Class<?>, Function<Trigger<?>, Trigger<?>>>builder()
          .put(
              Match.class,
              MatchPlayer.class,
              trigger -> new MatchToPlayer((Trigger<? super MatchPlayer>) trigger))
          .put(
              Match.class,
              Party.class,
              trigger -> new MatchToTeam((Trigger<? super Party>) trigger))
          .put(
              Party.class,
              Match.class,
              trigger -> new PartyToMatch((Trigger<? super Match>) trigger))
          .put(
              Party.class,
              MatchPlayer.class,
              trigger -> new PartyToPlayer((Trigger<? super MatchPlayer>) trigger))
          .put(
              MatchPlayer.class,
              Match.class,
              trigger -> new PlayerToMatch((Trigger<? super Match>) trigger))
          .put(
              MatchPlayer.class,
              Party.class,
              trigger -> new PlayerToTeam((Trigger<? super Party>) trigger))
          .build();

  @SuppressWarnings("unchecked")
  static <O, I> Trigger<? super O> modify(Trigger<? super I> child, Class<O> outer, Class<I> inner)
      throws InvalidXMLException {
    if (outer == inner) return (Trigger<? super O>) child;
    Function<Trigger<?>, Trigger<?>> converter = CONVERSIONS.get(outer, inner);
    if (converter == null) return null;
    return (Trigger<? super O>) converter.apply(child);
  }
}
