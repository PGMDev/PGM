package tc.oc.pgm.flag;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import org.bukkit.DyeColor;
import org.bukkit.util.Vector;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.points.PointParser;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.points.PointProviderAttributes;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class FlagParser {
  private final MapFactory factory;
  private final FilterParser filterParser;
  private final PointParser pointParser;

  private final List<Post> posts = new ArrayList<>();
  private final List<Net> nets = new ArrayList<>();
  private final List<FlagDefinition> flags = new ArrayList<>();

  public FlagParser(MapFactory factory) {
    this.factory = factory;
    this.filterParser = factory.getFilters();
    this.pointParser = new PointParser(factory);
  }

  private void checkDeprecatedFilter(Element el) throws InvalidXMLException {
    Node node = Node.fromChildOrAttr(el, "filter");
    if (node != null) {
      throw new InvalidXMLException(
          "'filter' is no longer supported, be more specific e.g. 'pickup-filter'", node);
    }
  }

  public Post parsePost(Element el) throws InvalidXMLException {
    checkDeprecatedFilter(el);

    @Nullable String name = el.getAttributeValue("name");
    String id = el.getAttributeValue("id");
    FeatureReference<TeamFactory> owner =
        factory.getFeatures().createReference(Node.fromAttr(el, "owner"), TeamFactory.class, null);
    boolean sequential = XMLUtils.parseBoolean(el.getAttribute("sequential"), false);
    boolean permanent = XMLUtils.parseBoolean(el.getAttribute("permanent"), false);
    double pointsPerSecond = XMLUtils.parseNumber(el.getAttribute("points-rate"), Double.class, 0D);
    Filter pickupFilter = filterParser.parseFilterProperty(el, "pickup-filter", StaticFilter.ALLOW);

    Duration recoverTime =
        XMLUtils.parseDuration(
            Node.fromAttr(el, "recover-time", "return-time"), Post.DEFAULT_RETURN_TIME);
    Duration respawnTime = XMLUtils.parseDuration(el.getAttribute("respawn-time"), null);
    Double respawnSpeed =
        XMLUtils.parseNumber(el.getAttribute("respawn-speed"), Double.class, (Double) null);
    ImmutableList<PointProvider> returnPoints =
        ImmutableList.copyOf(pointParser.parse(el, new PointProviderAttributes()));

    if (respawnTime == null && respawnSpeed == null) {
      respawnSpeed = Post.DEFAULT_RESPAWN_SPEED;
    }

    if (respawnTime != null && respawnSpeed != null) {
      throw new InvalidXMLException("post cannot have both respawn-time and respawn-speed", el);
    }

    if (returnPoints.isEmpty()) {
      throw new InvalidXMLException("post must have at least one point provider", el);
    }

    Post post =
        new Post(
            id,
            name, // Can be null
            owner,
            recoverTime,
            respawnTime,
            respawnSpeed,
            returnPoints,
            sequential,
            permanent,
            pointsPerSecond,
            pickupFilter);
    posts.add(post);
    factory.getFeatures().addFeature(el, post);

    return post;
  }

  public ImmutableSet<FlagDefinition> parseFlagSet(Node node) throws InvalidXMLException {
    ImmutableSet.Builder<FlagDefinition> flags = ImmutableSet.builder();
    for (String flagId : node.getValue().split("\\s")) {
      FlagDefinition flag = factory.getFeatures().get(flagId, FlagDefinition.class);
      if (flag == null) {
        throw new InvalidXMLException("No flag with ID '" + flagId + "'", node);
      }
      flags.add(flag);
    }
    return flags.build();
  }

  public Net parseNet(Element el, @Nullable FlagDefinition parentFlag) throws InvalidXMLException {
    checkDeprecatedFilter(el);

    String id = el.getAttributeValue("id");
    Region region = factory.getRegions().parseRequiredRegionProperty(el, "region");
    FeatureReference<TeamFactory> owner =
        factory.getFeatures().createReference(Node.fromAttr(el, "owner"), TeamFactory.class, null);
    double pointsPerCapture = XMLUtils.parseNumber(el.getAttribute("points"), Double.class, 0D);
    boolean sticky = XMLUtils.parseBoolean(el.getAttribute("sticky"), true);
    Filter captureFilter =
        filterParser.parseFilterProperty(el, "capture-filter", StaticFilter.ALLOW);
    Filter respawnFilter =
        filterParser.parseFilterProperty(el, "respawn-filter", StaticFilter.ALLOW);
    boolean respawnTogether = XMLUtils.parseBoolean(el.getAttribute("respawn-together"), false);
    Component respawnMessage = XMLUtils.parseFormattedText(el, "respawn-message");
    Component denyMessage = XMLUtils.parseFormattedText(el, "deny-message");
    Vector proximityLocation = XMLUtils.parseVector(el.getAttribute("location"), (Vector) null);

    Post returnPost = null;
    Node postAttr = Node.fromAttr(el, "post");
    if (postAttr != null) {
      // Posts are all parsed at this point, so we can do an immediate lookup
      returnPost = factory.getFeatures().get(postAttr.getValue(), Post.class);
      if (returnPost == null) {
        throw new InvalidXMLException("No post with ID '" + postAttr.getValue() + "'", postAttr);
      } else {
        returnPost.setSpecifiedPost(true);
      }
    }

    ImmutableSet<FlagDefinition> capturableFlags;
    Node flagsAttr = Node.fromAttr(el, "flag", "flags");
    if (flagsAttr != null) {
      if (parentFlag != null) {
        throw new InvalidXMLException(
            "Cannot specify flags on a net that is defined inside a flag", flagsAttr);
      }
      capturableFlags = this.parseFlagSet(flagsAttr);
    } else if (parentFlag != null) {
      capturableFlags = ImmutableSet.of(parentFlag);
    } else {
      capturableFlags = ImmutableSet.copyOf(this.flags);
    }

    if (capturableFlags.size() != 0 && returnPost != null) {
      for (FlagDefinition flagDef : flags) {
        if (capturableFlags.contains(flagDef)) {
          flagDef.setShowRespawnOnPickup(false);
        }
      }
    }

    ImmutableSet<FlagDefinition> returnableFlags;
    Node returnableNode = Node.fromAttr(el, "rescue", "return");
    if (returnableNode != null) {
      returnableFlags = this.parseFlagSet(returnableNode);
    } else {
      returnableFlags = ImmutableSet.of();
    }

    Net net =
        new Net(
            id,
            region,
            captureFilter,
            respawnFilter,
            owner,
            pointsPerCapture,
            sticky,
            denyMessage,
            respawnMessage,
            returnPost,
            capturableFlags,
            returnableFlags,
            respawnTogether,
            proximityLocation);
    nets.add(net);
    factory.getFeatures().addFeature(el, net);

    return net;
  }

  public FlagDefinition parseFlag(Element el) throws InvalidXMLException {
    checkDeprecatedFilter(el);

    String id = el.getAttributeValue("id");
    String name = el.getAttributeValue("name");
    boolean visible = XMLUtils.parseBoolean(el.getAttribute("show"), true);
    Boolean required = XMLUtils.parseBoolean(el.getAttribute("required"), null);
    DyeColor color = XMLUtils.parseDyeColor(el.getAttribute("color"), null);
    FeatureReference<TeamFactory> owner =
        factory.getFeatures().createReference(Node.fromAttr(el, "owner"), TeamFactory.class, null);
    double pointsPerCapture = XMLUtils.parseNumber(el.getAttribute("points"), Double.class, 0D);
    double pointsPerSecond = XMLUtils.parseNumber(el.getAttribute("points-rate"), Double.class, 0D);
    Filter pickupFilter = filterParser.parseFilterProperty(el, "pickup-filter", null);
    if (pickupFilter == null)
      pickupFilter = filterParser.parseFilterProperty(el, "filter", StaticFilter.ALLOW);
    Filter captureFilter =
        filterParser.parseFilterProperty(el, "capture-filter", StaticFilter.ALLOW);
    Kit pickupKit = factory.getKits().parseKitProperty(el, "pickup-kit", null);
    Kit dropKit = factory.getKits().parseKitProperty(el, "drop-kit", null);
    Kit carryKit = factory.getKits().parseKitProperty(el, "carry-kit", null);
    boolean multiCarrier = XMLUtils.parseBoolean(el.getAttribute("shared"), false);
    boolean sequential = XMLUtils.parseBoolean(el.getAttribute("sequential"), false);
    Component carryMessage = XMLUtils.parseFormattedText(el, "carry-message");
    boolean showRespawnOnPickup =
        XMLUtils.parseBoolean(el.getAttribute("show-respawn-on-pickup"), true);
    boolean dropOnWater = XMLUtils.parseBoolean(el.getAttribute("drop-on-water"), true);
    boolean showBeam = XMLUtils.parseBoolean(el.getAttribute("beam"), true);
    ProximityMetric flagProximityMetric =
        ProximityMetric.parse(
            el, "flag", new ProximityMetric(ProximityMetric.Type.CLOSEST_KILL, false));
    ProximityMetric netProximityMetric =
        ProximityMetric.parse(
            el, "net", new ProximityMetric(ProximityMetric.Type.CLOSEST_PLAYER, false));
    Post defaultPost;
    List<Post> flagPosts = new ArrayList<>();
    for (Element elPost : el.getChildren("post")) {
      flagPosts.add(this.parsePost(elPost));
    }

    if (!flagPosts.isEmpty()) {
      // Parse nested <post>
      defaultPost = flagPosts.get(0);
    } else {
      Node postAttr = Node.fromRequiredAttr(el, "post");
      defaultPost = factory.getFeatures().get(postAttr.getValue(), Post.class);
      flagPosts.add(defaultPost);
      if (defaultPost == null) {
        throw new InvalidXMLException("No post with ID '" + postAttr.getValue() + "'", postAttr);
      }
    }

    FlagDefinition flag =
        new FlagDefinition(
            id,
            name,
            required,
            visible,
            color,
            defaultPost,
            ImmutableList.copyOf(flagPosts),
            owner,
            pointsPerCapture,
            pointsPerSecond,
            pickupFilter,
            captureFilter,
            pickupKit,
            dropKit,
            carryKit,
            multiCarrier,
            carryMessage,
            dropOnWater,
            showBeam,
            flagProximityMetric,
            netProximityMetric,
            sequential,
            showRespawnOnPickup);
    flags.add(flag);
    factory.getFeatures().addFeature(el, flag);

    // Parse nested <net>s
    for (Element elNet : el.getChildren("net")) {
      this.parseNet(elNet, flag);
    }

    return flag;
  }

  public FlagModule parse(Document doc) throws InvalidXMLException {
    // Order of these is important to avoid the need for forward refs
    for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "flags", "post")) {
      this.parsePost(el);
    }

    for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "flags", "flag")) {
      this.parseFlag(el);
    }

    for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "flags", "net")) {
      this.parseNet(el, null);
    }

    return flags.isEmpty() ? null : new FlagModule(posts, nets, flags);
  }
}
