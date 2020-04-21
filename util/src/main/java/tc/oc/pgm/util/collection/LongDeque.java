package tc.oc.pgm.util.collection;

import gnu.trove.TLongCollection;
import gnu.trove.iterator.TLongIterator;
import java.util.NoSuchElementException;

/**
 * Minimal implementation of a FIFO queue of longs, stored efficiently in a primitive array. Does
 * not implement {@link gnu.trove.queue.TLongQueue} because it would be more trouble than its worth
 * to us.
 */
public class LongDeque {
  public static final int DEFAULT_CAPACITY = 16;

  private final long noEntryValue;
  private long[] buf;
  private int head, tail;

  public LongDeque() {
    this(DEFAULT_CAPACITY, 0);
  }

  public LongDeque(int capacity, long noEntryValue) {
    this.noEntryValue = noEntryValue;
    buf = new long[capacity];
  }

  private int advance(int index) {
    index++;
    return index < buf.length ? index : 0;
  }

  private void ensureCapacity(int delta) {
    int capacity = size() + delta;
    if (capacity >= buf.length) {
      // Find the smallest power of 2 that is big enough
      capacity |= (capacity >>> 1);
      capacity |= (capacity >>> 2);
      capacity |= (capacity >>> 4);
      capacity |= (capacity >>> 8);
      capacity |= (capacity >>> 16);
      capacity++;

      long[] bigger = new long[capacity];

      if (tail > head) {
        System.arraycopy(buf, head, bigger, 0, tail - head);
        tail -= head;
      } else {
        System.arraycopy(buf, head, bigger, 0, buf.length - head);
        System.arraycopy(buf, 0, bigger, buf.length - head, tail);
        tail += buf.length - head;
      }

      head = 0;
      buf = bigger;
    }
  }

  /** Return head */
  public long peek() {
    if (isEmpty()) {
      return getNoEntryValue();
    } else {
      return buf[head];
    }
  }

  /** Return head, throw if empty */
  public long element() {
    if (isEmpty()) throw new NoSuchElementException("queue is empty");
    return peek();
  }

  /** Remove from head */
  public long poll() {
    if (isEmpty()) {
      return getNoEntryValue();
    } else {
      long value = buf[head];
      head = advance(head);
      return value;
    }
  }

  /** Remove from head, throw if empty */
  public long remove() {
    if (isEmpty()) throw new NoSuchElementException("queue is empty");
    return poll();
  }

  private void addUnchecked(long value) {
    buf[tail] = value;
    tail = advance(tail);
  }

  /** Add to head */
  public void offer(long value) {
    add(value);
  }

  /** Add to head */
  public void add(long value) {
    ensureCapacity(1);
    addUnchecked(value);
  }

  public void addAll(TLongCollection values) {
    ensureCapacity(values.size());
    for (TLongIterator iter = values.iterator(); iter.hasNext(); ) {
      addUnchecked(iter.next());
    }
  }

  public void clear() {
    buf = new long[DEFAULT_CAPACITY];
    head = tail = 0;
  }

  public long getNoEntryValue() {
    return noEntryValue;
  }

  public int size() {
    int n = tail - head;
    if (n < 0) n += buf.length;
    return n;
  }

  public boolean isEmpty() {
    return head == tail;
  }

  public boolean contains(long entry) {
    for (int i = head; i != tail; i = advance(i)) {
      if (buf[i] == entry) return true;
    }
    return false;
  }
}
