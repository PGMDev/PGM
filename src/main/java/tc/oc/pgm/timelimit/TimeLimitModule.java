package tc.oc.pgm.timelimit;

import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.result.VictoryCondition;
import tc.oc.pgm.result.VictoryConditions;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.util.components.PeriodFormats;
import tc.oc.xml.InvalidXMLException;

public class TimeLimitModule implements MapModule {
  private final @Nullable TimeLimit timeLimit;

  public TimeLimitModule(@Nullable TimeLimit limit) {
    this.timeLimit = limit;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new TimeLimitMatchModule(match, this.timeLimit);
  }

  public static class Factory implements MapModuleFactory<TimeLimitModule> {
    @Override
    public TimeLimitModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      TimeLimit timeLimit = parseTimeLimit(factory, doc.getRootElement());
      timeLimit = parseLegacyTimeLimit(factory, doc.getRootElement(), "score", timeLimit);
      timeLimit = parseLegacyTimeLimit(factory, doc.getRootElement(), "blitz", timeLimit);

      // TimeLimitModule always loads
      return new TimeLimitModule(timeLimit);
    }

    private static @Nullable TimeLimit parseLegacyTimeLimit(
        MapFactory factory, Element el, String legacyTag, TimeLimit oldTimeLimit)
        throws InvalidXMLException {
      el = el.getChild(legacyTag);
      if (el != null) {
        TimeLimit newTimeLimit = parseTimeLimit(factory, el);
        if (newTimeLimit != null) {
          if (factory.getProto().isNoOlderThan(MapProtos.REMOVE_SCORE_TIME_LIMIT)) {
            throw new InvalidXMLException(
                "<time> inside <" + legacyTag + "> is no longer supported, use root <time> instead",
                el);
          }
          if (oldTimeLimit != null) {
            throw new InvalidXMLException(
                "Time limit conflicts with another one that is already defined", el);
          }
          return newTimeLimit;
        }
      }

      return oldTimeLimit;
    }

    private static @Nullable TimeLimit parseTimeLimit(MapFactory factory, Element el)
        throws InvalidXMLException {
      el = el.getChild("time");
      if (el == null) return null;

      return new TimeLimit(
          el.getAttributeValue("id"),
          PeriodFormats.SHORTHAND.parsePeriod(el.getTextNormalize()).toStandardDuration(),
          parseVictoryCondition(factory, el.getAttribute("result")),
          XMLUtils.parseBoolean(el.getAttribute("show"), true));
    }

    private static VictoryCondition parseVictoryCondition(MapFactory factory, Attribute attr)
        throws InvalidXMLException {
      if (attr == null) return null;
      try {
        return VictoryConditions.parse(factory, attr.getValue());
      } catch (IllegalArgumentException e) {
        throw new InvalidXMLException(e.getMessage(), attr);
      }
    }
  }
}
