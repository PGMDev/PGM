package tc.oc.pgm.trigger.triggers;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.util.function.Function;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.trigger.Trigger;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class ScopeSwitchTrigger<O, I> extends AbstractTrigger<O> {

  private final Function<O, I> single;
  private final Function<O, Iterable<I>> multi;
  private final Trigger<? super I> child;

  public ScopeSwitchTrigger(
      Class<O> outer,
      Function<O, I> single,
      Function<O, Iterable<I>> multi,
      Trigger<? super I> child) {
    super(outer);
    if ((single == null) == (multi == null))
      throw new IllegalArgumentException("One and only one of single or multi must be nonnull");
    this.single = single;
    this.multi = multi;
    this.child = child;
  }

  public static <O, I> Trigger<? super O> of(
      Trigger<? super I> child, Class<O> outer, Class<I> inner) throws InvalidXMLException {
    Function<O, I> single = TriggerModifiers.getSingleConversion(outer, inner);
    Function<O, Iterable<I>> multi = TriggerModifiers.getMultiConversion(outer, inner);

    // Either no conversion, or two conversions found!
    if ((single == null) == (multi == null)) return null;

    return new ScopeSwitchTrigger<>(outer, single, multi, child);
  }

  @Override
  public void trigger(O o) {
    if (single != null) {
      child.trigger(single.apply(o));
    } else if (multi != null) {
      Iterable<I> inner = multi.apply(o);
      for (I i : inner) {
        child.trigger(i);
      }
    }
  }

  @Override
  public void untrigger(O o) {
    if (single != null) {
      child.untrigger(single.apply(o));
    } else if (multi != null) {
      Iterable<I> inner = multi.apply(o);
      for (I i : inner) {
        child.untrigger(i);
      }
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private abstract static class TriggerModifiers {
    private static final Table<Class, Class, Function> SINGLE_CONVERSIONS;
    private static final Table<Class, Class, Function> MULTI_CONVERSIONS;

    static {
      ImmutableTable.Builder<Class, Class, Function> single = ImmutableTable.builder();
      registerSingle(single, MatchPlayer.class, Match.class, MatchPlayer::getMatch);
      registerSingle(single, MatchPlayer.class, Party.class, MatchPlayer::getParty);
      registerSingle(single, Party.class, Match.class, Party::getMatch);

      ImmutableTable.Builder<Class, Class, Function> multi = ImmutableTable.builder();
      registerMulti(multi, Match.class, Party.class, Match::getParties);
      registerMulti(multi, Match.class, MatchPlayer.class, Match::getPlayers);
      registerMulti(multi, Party.class, MatchPlayer.class, Party::getPlayers);

      SINGLE_CONVERSIONS = single.build();
      MULTI_CONVERSIONS = multi.build();
    }

    private static <O, I> void registerSingle(
        ImmutableTable.Builder<Class, Class, Function> single,
        Class<O> o,
        Class<I> i,
        Function<O, I> conv) {
      single.put(o, i, conv);
    }

    private static <O, I> void registerMulti(
        ImmutableTable.Builder<Class, Class, Function> multi,
        Class<O> o,
        Class<I> i,
        Function<O, Iterable<I>> conv) {
      multi.put(o, i, conv);
    }

    public static <O, I> Function<O, I> getSingleConversion(Class<O> outer, Class<I> inner) {
      if (outer == inner) return (Function<O, I>) Function.identity();
      return (Function<O, I>) SINGLE_CONVERSIONS.get(outer, inner);
    }

    public static <O, I> Function<O, Iterable<I>> getMultiConversion(
        Class<O> outer, Class<I> inner) {
      return (Function<O, Iterable<I>>) MULTI_CONVERSIONS.get(outer, inner);
    }
  }
}
