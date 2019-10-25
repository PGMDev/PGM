package tc.oc.pgm.broadcast;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.pgm.countdowns.CountdownRunner;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.modules.InfoModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(name = "Broadcast", requires = InfoModule.class)
public class BroadcastModule extends MapModule {
  private final Multimap<Duration, Broadcast> broadcasts;

  public BroadcastModule(Multimap<Duration, Broadcast> broadcasts) {
    this.broadcasts = broadcasts;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new BroadcastMatchModule(match, this.broadcasts);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static BroadcastModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    ArrayListMultimap<Duration, Broadcast> broadcasts = ArrayListMultimap.create();

    for (Element elBroadcasts : doc.getRootElement().getChildren("broadcasts")) {
      for (Element elBroadcast : elBroadcasts.getChildren()) {
        final Node nodeBroadcast = new Node(elBroadcast);
        Broadcast.Type type =
            XMLUtils.parseEnum(
                nodeBroadcast, elBroadcast.getName(), Broadcast.Type.class, "broadcast type");

        Component message = XMLUtils.parseFormattedText(nodeBroadcast);

        FilterParser filterParser = context.getFilterParser();
        Filter filter = filterParser.parseFilterProperty(elBroadcast, "filter");

        Duration after =
            XMLUtils.parseDuration(XMLUtils.getRequiredAttribute(elBroadcast, "after"));

        Attribute attrEvery = elBroadcast.getAttribute("every");
        Duration every = XMLUtils.parseDuration(attrEvery, null);

        Node countNode = Node.fromAttr(elBroadcast, "count");
        int count = XMLUtils.parseNumber(countNode, Integer.class, true, 1);

        if (count < 1) {
          throw new InvalidXMLException("Repeat count must be at least 1", countNode);
        }

        if (count > 1 && every == null) {
          // If a repeat count is specified but no interval, use the initial delay as the interval
          every = after;
        } else if (count == 1 && every != null) {
          // If a repeat interval is specified but no count, repeat forever
          count = Integer.MAX_VALUE;
        }

        if (every != null && every.isShorterThan(CountdownRunner.MIN_REPEAT_INTERVAL)) {
          throw new InvalidXMLException(
              "Repeat interval must be at least "
                  + CountdownRunner.MIN_REPEAT_INTERVAL.getMillis()
                  + " milliseconds",
              attrEvery);
        }

        broadcasts.put(after, new Broadcast(type, after, count, every, message, filter));
      }
    }

    return broadcasts.isEmpty() ? null : new BroadcastModule(broadcasts);
  }
}
