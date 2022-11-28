package tc.oc.pgm.broadcast;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.Duration;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.CountdownRunner;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class BroadcastModule implements MapModule<BroadcastMatchModule> {
  private final Multimap<Duration, Broadcast> broadcasts;

  public BroadcastModule(Multimap<Duration, Broadcast> broadcasts) {
    this.broadcasts = broadcasts;
  }

  @Override
  public BroadcastMatchModule createMatchModule(Match match) {
    return new BroadcastMatchModule(match, this.broadcasts);
  }

  public static class Factory implements MapModuleFactory<BroadcastModule> {
    @Override
    public BroadcastModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      ArrayListMultimap<Duration, Broadcast> broadcasts = ArrayListMultimap.create();

      for (Element elBroadcasts : doc.getRootElement().getChildren("broadcasts")) {
        for (Element elBroadcast : elBroadcasts.getChildren()) {
          final Node nodeBroadcast = new Node(elBroadcast);
          Broadcast.Type type =
              XMLUtils.parseEnum(
                  nodeBroadcast, elBroadcast.getName(), Broadcast.Type.class, "broadcast type");

          Component message = XMLUtils.parseFormattedText(nodeBroadcast);

          FilterParser filterParser = factory.getFilters();
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

          if (every != null
              && TimeUtils.isShorterThan(every, CountdownRunner.MIN_REPEAT_INTERVAL)) {
            throw new InvalidXMLException(
                "Repeat interval must be at least "
                    + CountdownRunner.MIN_REPEAT_INTERVAL.toMillis()
                    + " milliseconds",
                attrEvery);
          }

          broadcasts.put(after, new Broadcast(type, after, count, every, message, filter));
        }
      }

      return broadcasts.isEmpty() ? null : new BroadcastModule(broadcasts);
    }
  }
}
