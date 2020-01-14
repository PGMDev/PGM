package tc.oc.pgm.map;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bukkit.Difficulty;
import org.jdom2.Element;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.map.contrib.PlayerContributor;
import tc.oc.pgm.map.contrib.PseudonymContributor;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.util.SemanticVersion;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

import javax.annotation.Nullable;
import java.text.Normalizer;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class MapInfoImpl implements MapInfo {

  private final String id;
  private final SemanticVersion proto;
  private final SemanticVersion version;
  private final String name;
  private final String description;
  private final Collection<Contributor> authors;
  private final Collection<Contributor> contributors;
  private final Collection<String> rules;
  private final Difficulty difficulty;

  public MapInfoImpl(
      @Nullable String id,
      SemanticVersion proto,
      SemanticVersion version,
      String name,
      String description,
      @Nullable Collection<Contributor> authors,
      @Nullable Collection<Contributor> contributors,
      @Nullable Collection<String> rules,
      @Nullable Difficulty difficulty) {
    this.proto = checkNotNull(proto);
    this.version = checkNotNull(version);
    this.name = checkNotNull(name);
    this.description = checkNotNull(description);
    this.authors =
        authors == null || authors.isEmpty()
            ? Collections.emptyList()
            : ImmutableList.copyOf(authors);
    this.contributors =
        contributors == null || contributors.isEmpty()
            ? Collections.emptyList()
            : ImmutableList.copyOf(contributors);
    this.rules =
        rules == null || rules.isEmpty() ? Collections.emptyList() : ImmutableList.copyOf(rules);
    this.difficulty = difficulty == null ? Difficulty.NORMAL : difficulty;
    this.id = checkNotNull(normalizeName(id == null ? name : id));
  }

  public MapInfoImpl(MapInfo info) {
    this(checkNotNull(info).getId(), info.getProto(), info.getVersion(), info.getName(), info.getDescription(), info.getAuthors(), info.getContributors(), info.getRules(), info.getDifficulty());
  }

  public MapInfoImpl(Element root) throws InvalidXMLException {
    this(
        checkNotNull(root).getChildTextNormalize("slug"),
        XMLUtils.parseSemanticVersion(Node.fromRequiredAttr(root, "proto")),
        XMLUtils.parseSemanticVersion(Node.fromRequiredChildOrAttr(root, "version")),
        Node.fromRequiredChildOrAttr(root, "name").getValueNormalize(),
        Node.fromRequiredChildOrAttr(root, "objective", "description").getValueNormalize(),
        parseContributors(root, "author"),
        parseContributors(root, "contributor"),
        parseRules(root),
        XMLUtils.parseEnum(
            Node.fromLastChildOrAttr(root, "difficulty"), Difficulty.class, "difficulty"));
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public SemanticVersion getProto() {
    return proto;
  }

  @Override
  public SemanticVersion getVersion() {
    return version;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Collection<Contributor> getAuthors() {
    return authors;
  }

  @Override
  public Collection<Contributor> getContributors() {
    return contributors;
  }

  @Override
  public Collection<String> getRules() {
    return rules;
  }

  @Override
  public Difficulty getDifficulty() {
    return difficulty;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(getId()).build();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MapInfo)) return false;
    final MapInfo o = (MapInfo) obj;
    return new EqualsBuilder().append(getId(), o.getId()).build();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("id", getId()).build();
  }

  public static String normalizeName(@Nullable String idOrName) {
    return idOrName == null
        ? ""
        : Normalizer.normalize(idOrName, Normalizer.Form.NFD)
            .replaceAll("[^A-Za-z0-9]", "")
            .toLowerCase();
  }

  private static List<String> parseRules(Element root) {
    List<String> rules = null;
    for (Element parent : root.getChildren("rules")) {
      for (Element rule : parent.getChildren("rule")) {
        if (rules == null) {
          rules = new LinkedList<>();
        }
        rules.add(rule.getTextNormalize());
      }
    }
    return rules;
  }

  private static List<Contributor> parseContributors(Element root, String tag)
      throws InvalidXMLException {
    List<Contributor> contributors = null;
    for (Element parent : root.getChildren(tag + "s")) {
      for (Element child : parent.getChildren(tag)) {
        String name = XMLUtils.getNormalizedNullableText(child);
        UUID uuid = XMLUtils.parseUuid(Node.fromAttr(child, "uuid"));
        String contribution = XMLUtils.getNullableAttribute(child, "contribution", "contrib");

        if (name == null && uuid == null) {
          throw new InvalidXMLException("Contributor must have either a name or UUID", child);
        }

        if (contributors == null) {
          contributors = new LinkedList<>();
        }

        if (uuid == null) {
          contributors.add(new PseudonymContributor(name, contribution));
        } else {
          contributors.add(new PlayerContributor(uuid, contribution));
        }
      }
    }
    return contributors;
  }
}
