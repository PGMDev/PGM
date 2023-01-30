package tc.oc.pgm.goals;

import java.util.EnumSet;
import java.util.Set;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.parse.DynamicFilterValidation;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class ShowOptions {

  private final Set<ShowOption> options;
  private final Filter scoreboardFilter;

  private ShowOptions(Set<ShowOption> options, Filter scoreboardFilter) {
    this.options = options;
    this.scoreboardFilter = scoreboardFilter;
  }

  public static ShowOptions parse(FilterParser parser, Element el) throws InvalidXMLException {
    Set<ShowOption> options = EnumSet.noneOf(ShowOption.class);
    boolean show = XMLUtils.parseBoolean(el.getAttribute("show"), true);
    for (ShowOption option : ShowOption.values()) {
      if (XMLUtils.parseBoolean(el.getAttribute(option.getName()), show)) {
        options.add(option);
      }
    }
    Filter scoreboardFilter =
        parser.parseProperty(
            el, "scoreboard-filter", StaticFilter.ALLOW, DynamicFilterValidation.MATCH);
    return new ShowOptions(options, scoreboardFilter);
  }

  public boolean hasOption(ShowOption option) {
    return options.contains(option);
  }

  public Filter getScoreboardFilter() {
    return scoreboardFilter;
  }

  @Override
  public String toString() {
    return options.toString();
  }
}
