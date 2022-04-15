package tc.oc.pgm.flag.post;

import com.google.common.collect.ImmutableList;
import tc.oc.pgm.filters.query.GoalQuery;
import tc.oc.pgm.flag.Flag;

public class SequentialPostResolver implements PostResolver {
  private final ImmutableList<SinglePost> posts;
  private final SinglePost fallback;
  private int currentIdx;

  public SequentialPostResolver(ImmutableList<SinglePost> posts, SinglePost fallback) {
    this.posts = posts;
    this.fallback = fallback;
    this.currentIdx = posts.get(0) == fallback ? 0 : -1;
  }

  @Override
  public SinglePost get() {
    if (currentIdx == -1) return fallback;
    return posts.get(currentIdx);
  }

  @Override
  public SinglePost peekNext(Flag flag) {
    for (int offset = 1; offset < posts.size(); offset++) {
      final SinglePost post = posts.get((currentIdx + offset) % posts.size());
      if (post.getRespawnFilter().query(new GoalQuery(flag)).isAllowed()) return post;
    }
    return fallback;
  }

  @Override
  public SinglePost getNext(Flag flag) {
    for (int offset = 1; offset < posts.size(); offset++) {
      final SinglePost post = posts.get((currentIdx + offset) % posts.size());
      if (post.getRespawnFilter().query(new GoalQuery(flag)).isAllowed()) {
        this.currentIdx = (currentIdx + offset) % posts.size();
        return post;
      }
    }
    return fallback;
  }
}
