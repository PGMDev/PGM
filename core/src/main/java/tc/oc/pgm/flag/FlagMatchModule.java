package tc.oc.pgm.flag;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.flag.post.PostDefinition;
import tc.oc.pgm.goals.GoalMatchModule;

public class FlagMatchModule implements MatchModule {

  private final ImmutableMap<PostDefinition, Post> posts;
  private final ImmutableMap<FlagDefinition, Flag> flags;

  public FlagMatchModule(
      Match match,
      ImmutableList<PostDefinition> postDefinitions,
      ImmutableList<NetDefinition> nets,
      ImmutableList<FlagDefinition> flagDefinitions)
      throws ModuleLoadException {

    ImmutableMap.Builder<PostDefinition, Post> posts = ImmutableMap.builder();
    for (PostDefinition definition : postDefinitions) {
      Post post = new Post(match, definition);
      posts.put(definition, post);
      if (definition.getId() != null) match.getFeatureContext().add(post);
    }
    this.posts = posts.build();

    ImmutableMap.Builder<FlagDefinition, Flag> flags = ImmutableMap.builder();
    for (FlagDefinition definition : flagDefinitions) {
      ImmutableSet.Builder<NetDefinition> netsBuilder = ImmutableSet.builder();
      for (NetDefinition net : nets) {
        if (net.getCapturableFlags().contains(definition)) {
          netsBuilder.add(net);
        }
      }

      Flag flag = new Flag(match, definition, netsBuilder.build());
      flags.put(definition, flag);
      match.getFeatureContext().add(flag);
      match.needModule(GoalMatchModule.class).addGoal(flag);
    }
    this.flags = flags.build();
  }

  @Override
  public void load() {
    for (Flag flag : this.flags.values()) {
      flag.load(this);
    }
  }

  public ImmutableCollection<Flag> getFlags() {
    return flags.values();
  }

  public Post getPost(PostDefinition postDefinition) {
    return posts.get(postDefinition);
  }

  public ImmutableCollection<Post> getPosts() {
    return posts.values();
  }
}
