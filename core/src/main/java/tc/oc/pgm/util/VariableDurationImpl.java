package tc.oc.pgm.util;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import com.google.common.collect.Range;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.VariableDuration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.range.Ranges;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public record VariableDurationImpl(Duration duration, List<Rule> rules)
    implements VariableDuration {
  public VariableDurationImpl {
    rules = List.copyOf(rules);
  }

  private record Rule(Duration duration, Range<Integer> players, Range<Duration> matchLength) {}

  @Override
  public Duration getDuration(Match match) {
    int onlinePlayers = Bukkit.getOnlinePlayers().size();
    Duration matchLength = match.getDuration();
    for (Rule rule : rules) {
      if (rule.players.contains(onlinePlayers) && rule.matchLength.contains(matchLength)) {
        return rule.duration;
      }
    }
    return duration;
  }

  @Override
  public Duration getDefault() {
    return duration;
  }

  public static VariableDuration of(Duration duration) throws TextException {
    return new VariableDurationImpl(duration, List.of());
  }

  public static VariableDuration parse(ConfigurationSection section) throws TextException {
    Duration defaultDuration = parseDuration(section.getString("default", "30s"));
    List<Rule> rules = new ArrayList<>();
    for (var ruleMap : section.getMapList("rules")) {
      var duration = parseDuration(String.valueOf(ruleMap.get("duration")));
      var players = parseRange(ruleMap, "players", TextParser::parseInteger);
      var matchLength = parseRange(ruleMap, "match-length", TextParser::parseDuration);
      rules.add(new Rule(duration, players, matchLength));
    }
    return new VariableDurationImpl(defaultDuration, rules);
  }

  private static <T extends Comparable<T>> Range<T> parseRange(
      Map<?, ?> cfg, String key, Function<String, T> parser) throws TextException {
    var min = cfg.get("min-" + key);
    var max = cfg.get("max-" + key);
    return Ranges.createClosedRange(
        min == null ? null : parser.apply(String.valueOf(min)),
        max == null ? null : parser.apply(String.valueOf(max)));
  }
}
