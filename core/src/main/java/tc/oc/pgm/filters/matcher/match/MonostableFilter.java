package tc.oc.pgm.filters.matcher.match;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.text.TemporalComponent.clock;
import static tc.oc.pgm.util.text.TemporalComponent.seconds;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.filter.ReactorFactory;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.filters.operator.AllFilter;
import tc.oc.pgm.filters.operator.InverseFilter;
import tc.oc.pgm.filters.operator.SingleFilterFunction;

public class MonostableFilter extends SingleFilterFunction
    implements TypedFilter<MatchQuery>, ReactorFactory<MonostableFilter.Reactor<?>> {

  private final Duration duration;
  private final @Nullable Component message;
  private final boolean colons;

  public static Filter afterMatchStart(Duration duration) {
    return after(MatchPhaseFilter.RUNNING, duration, null);
  }

  /**
   * Will rise after {@code duration} has passed after {@code filter} starts to rise.
   *
   * @param filter the filter to listen for changes to
   * @param duration the duration to delay this filters rise
   */
  public static Filter after(Filter filter, Duration duration, @Nullable Component message) {
    return AllFilter.of(filter, new InverseFilter(new MonostableFilter(filter, duration, message)));
  }

  public MonostableFilter(Filter filter, Duration duration, @Nullable Component message) {
    super(filter);
    this.duration = duration;
    this.message = message == null ? null : message.colorIfAbsent(NamedTextColor.YELLOW);
    this.colons = duration.getSeconds() >= 90;
  }

  @Override
  public Class<MatchQuery> queryType() {
    return MatchQuery.class;
  }

  @Override
  public boolean matches(MatchQuery query) {
    return query.reactor(this).matches(query);
  }

  @Override
  public Reactor<?> createReactor(Match match, FilterMatchModule fmm) {
    return message == null
        ? new Reactor<>(match, fmm, Filterables.scope(filter))
        : new BossbarReactor<>(match, fmm, Filterables.scope(filter));
  }

  protected class Reactor<F extends Filterable<?>> extends ReactorFactory.Reactor
      implements Tickable {

    protected final Class<F> scope;

    // Filterables that currently pass the inner filter, mapped to the instants that they expire.
    // They are not actually removed until the inner filter goes false.
    protected final Map<Filterable<?>, Instant> endTimes = new HashMap<>();

    public Reactor(Match match, FilterMatchModule fmm, Class<F> scope) {
      super(match, fmm);
      this.scope = scope;
      match.addTickable(this, MatchScope.LOADED);
      fmm.onChange(scope, filter, this::matches);
    }

    boolean matches(MatchQuery query) {
      final Filterable<?> filterable = query.filterable(this.scope);
      if (filterable == null) return false;

      return matches(filterable, filter.response(query));
    }

    boolean matches(Filterable<?> filterable, boolean response) {
      if (response) { // If inner filter still matches, check if the time has expired
        final Instant now = this.match.getTick().instant;

        Instant end = endTimes.get(filterable);
        if (end == null) {
          // Cannot use computeIfAbsent, as we want invalidation after put
          endTimes.put(filterable, end = now.plus(duration));
          this.invalidate(filterable);
        }
        return now.isBefore(end);
      } else {
        if (endTimes.remove(filterable) != null) {
          this.invalidate(filterable);
        }
        return false;
      }
    }

    @Override
    public void tick(Match match, Tick tick) {
      final Instant now = tick.instant;

      endTimes.forEach(
          (filterable, end) -> {
            if (now.isAfter(end)) {
              this.invalidate(filterable);
            }
          });
    }
  }

  protected class BossbarReactor<F extends Filterable<?>> extends Reactor<F> {

    final Component message;
    final Map<Filterable<?>, BossBar> bars;

    Instant lastTick = Instant.MIN;
    // Used to specify that a bulk invalidation update is ongoing, so player-specific invalidations
    // can be ignored
    boolean bulkUpdate;

    public BossbarReactor(Match match, FilterMatchModule fmm, Class<F> scope) {
      super(match, fmm, scope);

      this.message = assertNotNull(MonostableFilter.this.message);
      this.bars = new HashMap<>();

      // When the scope isn't a player, a player may stop passing while the scope itself still does.
      // Eg: the scope is team, and it passes, but a player switches team.
      // In that case, the player that stops passing the filter should get the bossbar added, or
      // removed.
      if (scope != MatchPlayer.class)
        fmm.onChange(MatchPlayer.class, MonostableFilter.this, this::updatePlayerBar);
    }

    @Override
    protected void invalidate(Filterable<?> filterable) {
      bulkUpdate = true;
      super.invalidate(filterable);
      bulkUpdate = false;

      Instant end = endTimes.get(filterable);

      // Create or remove boss bar
      if (end != null && match.getTick().instant.isBefore(end)) createBossBar(filterable);
      else removeBossBar(filterable);
    }

    @Override
    public void tick(Match match, Tick tick) {
      super.tick(match, tick);

      final Instant now = tick.instant;

      endTimes.forEach(
          (filterable, end) -> {
            if (now.isBefore(end)) {
              // If the entry is still valid, check if its elapsed time crossed a second
              // boundary over the last tick, and update the boss bar if it did.
              long oldSeconds = lastTick.until(end, ChronoUnit.SECONDS);
              long newSeconds = now.until(end, ChronoUnit.SECONDS);

              // Round up as going from 4s to 3.95s should show 4s
              if (oldSeconds != newSeconds)
                updateBossBar(filterable, Duration.ofSeconds(newSeconds + 1));
            }
          });

      lastTick = now;
    }

    private void createBossBar(Filterable<?> filterable) {
      Component message = getMessage(duration);

      BossBar bar =
          bars.computeIfAbsent(
              filterable,
              f -> BossBar.bossBar(message, 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));

      bar.name(message).progress(1f);
      filterable.showBossBar(bar);
    }

    private void updateBossBar(Filterable<?> filterable, Duration remaining) {
      Component msg = getMessage(remaining);
      float progress = progress(remaining);

      bars.computeIfPresent(filterable, (f, b) -> b.name(msg).progress(progress));
    }

    private void removeBossBar(Filterable<?> filterable) {
      BossBar bar = bars.remove(filterable);
      if (bar != null) filterable.hideBossBar(bar);
    }

    private void updatePlayerBar(MatchPlayer player, boolean response) {
      // Ignore player-specific updates during a bulk update
      if (bulkUpdate) return;

      BossBar bar = response ? bars.get(player.getFilterableAncestor(scope)) : null;

      // We need to hide all other scope's bars from the player, otherwise the player may keep a bar
      // from a different scope (eg: team) it no longer is part of.

      for (BossBar otherBar : bars.values()) {
        if (bar != otherBar) player.hideBossBar(otherBar);
      }
      // And finally ensure the player is added to the bar of their current scope, if any.
      if (bar != null) player.showBossBar(bar);
    }

    private Component getMessage(Duration remaining) {
      Component duration =
          colons
              ? clock(remaining).color(NamedTextColor.AQUA)
              : seconds(remaining.getSeconds(), NamedTextColor.AQUA);

      return message.replaceText(
          TextReplacementConfig.builder().once().matchLiteral("{0}").replacement(duration).build());
    }

    private float progress(Duration remaining) {
      return Math.min(1f, (float) remaining.getSeconds() / duration.getSeconds());
    }
  }
}
