package tc.oc.pgm.timelimit;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.result.VictoryConditions;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class TimeLimitModule implements MapModule<TimeLimitMatchModule> {
  private static final Collection<MapTag> TAGS =
      ImmutableList.of(new MapTag("timelimit", "Timelimit", false, true));
  private final @Nullable TimeLimit timeLimit;

  public TimeLimitModule(@Nullable TimeLimit limit) {
    this.timeLimit = limit;
  }

  @Override
  public Collection<MapTag> getTags() {
    return timeLimit == null ? Collections.emptyList() : TAGS;
  }

  @Override
  public TimeLimitMatchModule createMatchModule(Match match) {
    return new TimeLimitMatchModule(match, this.timeLimit);
  }

  public static class Factory implements MapModuleFactory<TimeLimitModule> {

    @Nullable
    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(TeamModule.class);
    }

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
          TextParser.parseDuration(el.getTextNormalize()),
          XMLUtils.parseDuration(el.getAttribute("overtime")),
          XMLUtils.parseDuration(el.getAttribute("max-overtime")),
          XMLUtils.parseDuration(el.getAttribute("end-overtime")),
          parseVictoryCondition(factory, el.getAttribute("result")),
          XMLUtils.parseBoolean(el.getAttribute("show"), true));
    }

    private static VictoryCondition parseVictoryCondition(MapFactory factory, Attribute attr)
        throws InvalidXMLException {
      if (attr == null) return null;
      try {
        return VictoryConditions.parseNullable(factory, attr.getValue());
      } catch (TextException e) {
        throw new InvalidXMLException(e.getLocalizedMessage(), attr);
      }
    }
  }
}
