package tc.oc.pgm.timelimit;

import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.bossbar.BossBarModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.map.ProtoVersions;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.result.VictoryCondition;
import tc.oc.pgm.result.VictoryConditions;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.util.components.PeriodFormats;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Time Limit",
    requires = {BossBarModule.class})
public class TimeLimitModule extends MapModule<TimeLimitMatchModule> {

  private static final MapTag TIMELIMIT_TAG = MapTag.forName("timelimit");

  private final @Nullable TimeLimit timeLimit;

  public TimeLimitModule(@Nullable TimeLimit limit) {
    this.timeLimit = limit;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void loadTags(Set tags) {
    if (timeLimit != null) tags.add(TIMELIMIT_TAG);
  }

  @Override
  public TimeLimitMatchModule createMatchModule(Match match) {
    return new TimeLimitMatchModule(match, this.timeLimit);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static TimeLimitModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    TimeLimit timeLimit = parseTimeLimit(context, doc.getRootElement());
    timeLimit = parseLegacyTimeLimit(context, doc.getRootElement(), "score", timeLimit);
    timeLimit = parseLegacyTimeLimit(context, doc.getRootElement(), "blitz", timeLimit);

    // TimeLimitModule always loads
    return new TimeLimitModule(timeLimit);
  }

  private static @Nullable TimeLimit parseLegacyTimeLimit(
      MapModuleContext context, Element el, String legacyTag, TimeLimit oldTimeLimit)
      throws InvalidXMLException {
    el = el.getChild(legacyTag);
    if (el != null) {
      TimeLimit newTimeLimit = parseTimeLimit(context, el);
      if (newTimeLimit != null) {
        if (context.getProto().isNoOlderThan(ProtoVersions.REMOVE_SCORE_TIME_LIMIT)) {
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

  private static @Nullable TimeLimit parseTimeLimit(MapModuleContext context, Element el)
      throws InvalidXMLException {
    el = el.getChild("time");
    if (el == null) return null;

    return new TimeLimit(
        el.getAttributeValue("id"),
        PeriodFormats.SHORTHAND.parsePeriod(el.getTextNormalize()).toStandardDuration(),
        parseVictoryCondition(context, el.getAttribute("result")),
        XMLUtils.parseBoolean(el.getAttribute("show"), true));
  }

  private static VictoryCondition parseVictoryCondition(MapModuleContext context, Attribute attr)
      throws InvalidXMLException {
    if (attr == null) return null;
    try {
      return VictoryConditions.parse(context, attr.getValue());
    } catch (IllegalArgumentException e) {
      throw new InvalidXMLException(e.getMessage(), attr);
    }
  }
}
