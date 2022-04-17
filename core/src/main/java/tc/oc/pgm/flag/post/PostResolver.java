package tc.oc.pgm.flag.post;

import java.util.function.Supplier;
import tc.oc.pgm.flag.Flag;

public interface PostResolver extends Supplier<SinglePost> {
  SinglePost get();

  default SinglePost peekNext(Flag flag) {
    return get();
  }

  default SinglePost getNext(Flag flag) {
    return get();
  }
}
