package tc.oc.pgm.flag.post;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;

public class CompositePost extends PostDefinition {

  private final ImmutableList<SinglePost> posts;
  private final SinglePost fallback;
  private final boolean sequential;

  public CompositePost(
      String id,
      boolean sequential,
      ImmutableList<SinglePost> posts,
      @Nullable SinglePost fallback) {
    super(id);

    this.sequential = sequential;
    this.posts = posts;
    this.fallback = fallback != null ? fallback : posts.get(0);
  }

  public boolean isSequential() {
    return sequential;
  }

  public ImmutableList<SinglePost> getPosts() {
    return posts;
  }

  public SinglePost getFallback() {
    return fallback;
  }

  @Override
  public PostResolver createResolver(Match match) {
    return sequential
        ? new SequentialPostResolver(posts, fallback)
        : new RandomOrderPostResolver(posts, fallback);
  }
}
