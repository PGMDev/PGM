package tc.oc.pgm.flag.post;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import tc.oc.pgm.filters.query.GoalQuery;
import tc.oc.pgm.flag.Flag;

public class RandomOrderPostResolver implements PostResolver {
  private final List<SinglePost> posts;
  private final SinglePost fallback;
  private SinglePost current, next;

  public RandomOrderPostResolver(ImmutableList<SinglePost> posts, SinglePost fallback) {
    this.posts = new ArrayList<>(posts);
    this.fallback = fallback;
    this.current = fallback;
  }

  @Override
  public SinglePost get() {
    return current;
  }

  @Override
  public SinglePost peekNext(Flag flag) {
    this.next = findNext(flag);
    return next == null ? fallback : next;
  }

  @Override
  public SinglePost getNext(Flag flag) {
    this.next = findNext(flag);
    if (this.next == null) return this.current = fallback;

    this.current = this.next;
    this.next = null;
    return this.current;
  }

  private SinglePost findNext(Flag flag) {
    GoalQuery query = new GoalQuery(flag);
    if (next != null && next.getRespawnFilter().query(new GoalQuery(flag)).isAllowed()) return next;

    Collections.shuffle(posts);
    for (SinglePost post : posts) {
      if (post.getRespawnFilter().query(query).isAllowed()) return post;
    }
    return null;
  }
}
