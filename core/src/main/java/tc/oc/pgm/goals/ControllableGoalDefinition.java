package tc.oc.pgm.goals;

import javax.annotation.Nullable;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public abstract class ControllableGoalDefinition extends GoalDefinition {

  private final Filter controlFilter;

  private final Filter dominateFilter;

  public enum CaptureCondition {
    EXCLUSIVE, // Team owns all players on the point
    MAJORITY, // Team owns more than half the players on the point
    LEAD // Team owns more players on the point than any other single team
  }

  private final CaptureCondition captureCondition;

  private final boolean permanent;

  private final boolean showProgress;

  public ControllableGoalDefinition(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      boolean visible,
      Filter controlFilter,
      Filter dominateFilter,
      CaptureCondition captureCondition,
      boolean permanent,
      boolean showProgress) {
    super(id, name, required, visible);
    this.controlFilter = controlFilter;
    this.dominateFilter = dominateFilter;
    this.captureCondition = captureCondition;
    this.permanent = permanent;
    this.showProgress = showProgress;
  }

  public static CaptureCondition parseCaptureCondition(Element el) throws InvalidXMLException {
    return XMLUtils.parseEnum(
        Node.fromAttr(el, "capture-rule"),
        CaptureCondition.class,
        "capture rule",
        CaptureCondition.EXCLUSIVE);
  }

  public Filter getControlFilter() {
    return controlFilter;
  }

  public Filter getDominateFilter() {
    return dominateFilter;
  }

  public CaptureCondition getCaptureCondition() {
    return captureCondition;
  }

  /** If the goal is permanent it can only be captured once */
  public boolean isPermanent() {
    return permanent;
  }

  public boolean shouldShowProgress() {
    return showProgress;
  }
}
