package tc.oc.pgm.util.material;

import static tc.oc.pgm.util.material.MaterialUtils.MATERIAL_UTILS;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.material.matcher.CompoundMaterialMatcher;
import tc.oc.pgm.util.material.matcher.MultipleMaterialMatcher;
import tc.oc.pgm.util.material.matcher.SingularMaterialMatcher;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

/** A predicate on world */
public interface MaterialMatcher {

  boolean matches(Material material);

  default boolean matches(Block block) {
    return matches(MaterialData.block(block));
  }

  default boolean matches(BlockState block) {
    return matches(MaterialData.block(block));
  }

  boolean matches(MaterialData materialData);

  boolean matches(ItemStack stack);

  /**
   * Iterates over ALL matching {@link Material}s. This can be a long list if the matching criteria
   * is very broad.
   */
  Set<Material> getMaterials();

  Set<BlockMaterialData> getPossibleBlocks();

  static MaterialMatcher of(Material material) {
    return SingularMaterialMatcher.of(material);
  }

  static MaterialMatcher of(Material... materials) {
    return MultipleMaterialMatcher.of(Arrays.asList(materials));
  }

  static MaterialMatcher of(Collection<Material> materials) {
    return MultipleMaterialMatcher.of(materials);
  }

  static MaterialMatcher of(Predicate<Material> materials) {
    return builder().addAll(materials).build();
  }

  static MaterialMatcher ofMatchers(Collection<? extends MaterialMatcher> matchers) {
    return CompoundMaterialMatcher.of(matchers);
  }

  static MaterialMatcher parse(Element el) throws InvalidXMLException {
    return MaterialMatcher.builder().parse(new Node(el)).build();
  }

  static MaterialMatcher.Builder builder() {
    return MATERIAL_UTILS.matcherBuilder();
  }

  interface Singular extends MaterialMatcher {
    Material getMaterial();
  }

  interface Builder {
    /**
     * Set the builder to only accept materials, error on material:data syntax
     *
     * @return this
     */
    Builder materialsOnly();

    /**
     * Set the builder to accept multi-block patterns, eg: wool;planks
     *
     * @return this
     */
    Builder multiPattern();

    default Builder parse(@Nullable Element el) throws InvalidXMLException {
      return parse(Node.fromNullable(el));
    }

    /**
     * Parse the given node
     *
     * @param node the node to parse, null will be ignored
     * @return this
     * @throws InvalidXMLException if the material is invalid or the format not allowed
     */
    Builder parse(@Nullable Node node) throws InvalidXMLException;

    /**
     * Parse the given material
     *
     * @param material the
     * @param node node to use for error reporting, otherwise ignored
     * @return this
     * @throws InvalidXMLException if the material is invalid or the format not allowed
     */
    Builder parse(String material, @Nullable Node node) throws InvalidXMLException;

    Builder add(Material material);

    default Builder addNullable(Material material) {
      if (material != null) return add(material);
      return this;
    }

    Builder add(Material material, boolean flatten);

    Builder add(ItemStack item, boolean flatten);

    Builder addAll(Material... material);

    Builder addAll(Collection<Material> material);

    Builder addAll(Predicate<Material> materialPredicate);

    Builder add(MaterialMatcher matcher);

    boolean isEmpty();

    MaterialMatcher ifEmpty(MaterialMatcher def);

    MaterialMatcher build();
  }

  abstract class BuilderImpl implements MaterialMatcher.Builder {
    protected boolean materialsOnly = false;
    protected boolean multiPattern = false;
    protected EnumSet<Material> materials = EnumSet.noneOf(Material.class);
    protected Set<MaterialMatcher> matchers = new HashSet<>();

    @Override
    public MaterialMatcher.Builder materialsOnly() {
      this.materialsOnly = true;
      return this;
    }

    @Override
    public Builder multiPattern() {
      this.multiPattern = true;
      return this;
    }

    @Override
    public Builder parse(Node node) throws InvalidXMLException {
      if (node != null) parse(node.getValueNormalize(), node);
      return this;
    }

    public Builder parse(String text, @Nullable Node node) throws InvalidXMLException {
      if (multiPattern) {
        for (String value : Splitter.on(";").split(text)) {
          parseSingle(value, node);
        }
      } else {
        parseSingle(text, node);
      }
      return this;
    }

    protected abstract void parseSingle(String text, @Nullable Node node)
        throws InvalidXMLException;

    @Override
    public MaterialMatcher.Builder add(Material material) {
      this.materials.add(material);
      return this;
    }

    @Override
    public MaterialMatcher.Builder addAll(Material... materials) {
      this.materials.addAll(Arrays.asList(materials));
      return this;
    }

    @Override
    public MaterialMatcher.Builder addAll(Collection<Material> materials) {
      this.materials.addAll(materials);
      return this;
    }

    @Override
    public Builder addAll(Predicate<Material> materialPredicate) {
      for (Material m : Material.values()) {
        if (materialPredicate.test(m)) this.materials.add(m);
      }
      return this;
    }

    @Override
    public Builder add(MaterialMatcher matcher) {
      // Already part of the matcher in form of material, can ignore
      if (matcher instanceof Singular s && materials.contains(s.getMaterial())) return this;
      if (matcher instanceof SingularMaterialMatcher
          || matcher instanceof MultipleMaterialMatcher) {
        Set<Material> toAdd = matcher.getMaterials();
        materials.addAll(toAdd);
        // Ignore any matcher already handled by our generic material list
        matchers.removeIf(m -> m instanceof Singular s && toAdd.contains(s.getMaterial()));
        return this;
      }
      matchers.add(matcher);
      return this;
    }

    @Override
    public boolean isEmpty() {
      return materials.isEmpty() && matchers.isEmpty();
    }

    @Override
    public MaterialMatcher ifEmpty(MaterialMatcher def) {
      return isEmpty() ? def : build();
    }

    @Override
    public MaterialMatcher build() {
      List<MaterialMatcher> materialMatchers = new ArrayList<>(materials.size() + matchers.size());
      if (!materials.isEmpty()) materialMatchers.add(MaterialMatcher.of(materials));
      if (!matchers.isEmpty()) materialMatchers.addAll(matchers);
      return MaterialMatcher.ofMatchers(materialMatchers);
    }
  }
}
