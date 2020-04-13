package tc.oc.pgm.timelimit;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.result.VictoryConditions;
import tc.oc.util.TimeUtils;
import tc.oc.util.xml.InvalidXMLException;
import tc.oc.util.xml.XMLUtils;

public class TimeLimitModule implements MapModule {
  private static final Collection<MapTag> TAGS =
      ImmutableList.of(MapTag.create("timelimit", "Timelimit", false, true));
  private final @Nullable TimeLimit timeLimit;

  public TimeLimitModule(@Nullable TimeLimit limit) {
    this.timeLimit = limit;
  }

  @Override
  public Collection<MapTag> getTags() {
    return timeLimit == null ? Collections.emptyList() : TAGS;
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
          TimeUtils.parseDuration(el.getTextNormalize()),
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
