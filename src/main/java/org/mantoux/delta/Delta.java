package org.mantoux.delta;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.stream.Collectors.toList;
import static org.mantoux.delta.Op.Type.DELETE;
import static org.mantoux.delta.Op.Type.INSERT;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@JsonInclude(value = NON_NULL)
public class Delta extends ArrayList<Op> {

  public Delta(Collection<Op> other) {
    super(other);
  }

  public Delta() {
    super();
  }

  @Override
  public Iterator iterator() {
    return new Iterator(this);
  }

  private void insertFirst(Op element) {
    add(0, element);
  }

  public Delta filter(Predicate<Op> predicate) {
    return new Delta(stream().filter(predicate).collect(toList()));
  }

  public Delta insert(Object arg, AttributeMap attributes) {
    if (arg == null) return this;
    // 0x200b is NOT a white space character
    if (arg instanceof String string && string.isEmpty()) return this;
    return push(Op.insert(arg, attributes));
  }

  public Delta insert(String arg) {
    return insert(arg, null);
  }

  public Delta insert(Map<String, Object> object) {
    return insert(object, null);
  }

  public Delta delete(int length) {
    if (length <= 0) return this;
    return push(Op.delete(length));
  }

  public Delta retain(int length, AttributeMap attributes) {
    if (length <= 0) return this;
    return push(Op.retain(length, attributes));
  }

  public Delta retain(int length) {
    return retain(length, null);
  }

  // TODO : P1 - TEST
  public Delta push(Op newOp) {
    if (isEmpty()) {
      add(newOp);
      return this;
    }
    int index = size();
    Op lastOp = get(index - 1);
    newOp = newOp.copy();
    if (newOp.isDelete() && lastOp.isDelete()) {
      set(index - 1, Op.delete(lastOp.length() + newOp.length()));
      return this;
    }
    // Since it does not matter if we insert before or after deleting at the same index,
    // always prefer to insert first
    if (lastOp.isDelete() && newOp.isInsert()) {
      index -= 1;
      if (index == 0) {
        insertFirst(newOp);
        return this;
      }
      lastOp = get(index - 1);
    }

    if (Objects.equals(newOp.attributes(), lastOp.attributes())) {
      if (newOp.isInsert() && lastOp.isInsert()) {
        if (newOp.arg() instanceof String && lastOp.arg() instanceof String) {
          final Op mergedOp =
              Op.insert(lastOp.argAsString() + newOp.argAsString(), newOp.attributes());
          set(index - 1, mergedOp);
          return this;
        }
      }
      if (lastOp.isRetain() && newOp.isRetain()) {
        final Op mergedOp = Op.retain(lastOp.length() + newOp.length(), newOp.attributes());
        set(index - 1, mergedOp);
        return this;
      }
    }
    if (index == size()) add(newOp);
    else add(index, newOp);
    return this;
  }

  public Delta chop() {
    if (isEmpty()) return this;
    Op lastOp = getLast();
    if (lastOp.isRetain() && lastOp.attributes() == null) {
      removeLast();
    }
    return this;
  }

  public <T> List<T> map(Function<Op, T> mapper) {
    return stream().map(mapper).collect(Collectors.toList());
  }

  public List<Op>[] partition(Predicate<Op> predicate) {
    final Delta passed = new Delta();
    final Delta failed = new Delta();
    forEach(
        op -> {
          if (predicate.test(op)) {
            passed.add(op);
          } else {
            failed.add(op);
          }
        });
    return new Delta[] {passed, failed};
  }

  public <T> T reduce(T initialValue, BiFunction<T, Op, T> accumulator) {
    return stream().reduce(initialValue, accumulator, (value1, value2) -> value2);
  }

  public int changeLength() {
    return reduce(
        0,
        (length, op) -> {
          if (op.isInsert()) return length + op.length();
          if (op.isDelete()) return length - op.length();
          return length;
        });
  }

  public int length() {
    return reduce(0, (length, op) -> length + op.length());
  }

  public Delta slice(int start) {
    return slice(start, Integer.MAX_VALUE);
  }

  public Delta compose(Delta other) {
    final Delta.Iterator it = iterator();
    final Delta.Iterator otherIt = other.iterator();

    final Delta combined = new Delta();
    final Op firstOther = otherIt.peek();
    if (firstOther != null && firstOther.isRetain() && firstOther.attributes() == null) {
      int firstLeft = firstOther.length();
      while (it.peekType() == INSERT && it.peekLength() <= firstLeft) {
        firstLeft -= it.peekLength();
        combined.add(it.next());
      }
      if (firstOther.length() - firstLeft > 0) otherIt.next(firstOther.length() - firstLeft);
    }
    final Delta delta = new Delta(combined);

    while (it.hasNext() || otherIt.hasNext()) {

      if (otherIt.peekType() == INSERT) delta.push(otherIt.next());
      else if (it.peekType() == DELETE) delta.push(it.next());
      else {

        final int length = Math.min(it.peekLength(), otherIt.peekLength());
        final Op thisOp = it.next(length);
        final Op otherOp = otherIt.next(length);

        if (otherOp.isRetain()) {
          Op newOp;
          // Preserve null when composing with a retain, otherwise remove it for inserts
          AttributeMap attributes =
              AttributeMap.compose(thisOp.attributes(), otherOp.attributes(), thisOp.isRetain());
          if (thisOp.isRetain()) newOp = Op.retain(length, attributes);
          else newOp = Op.insert(thisOp.arg(), attributes);
          delta.push(newOp);
          // Optimization if rest of other is just retain
          if (!otherIt.hasNext() && delta.getLast().equals(newOp)) {
            final Delta rest = new Delta(it.rest());
            return delta.concat(rest).chop();
          }
        } else if (otherOp.isDelete() && thisOp.isRetain()) {
          delta.push(otherOp);
        }
      }
    }
    return delta.chop();
  }

  public void eachLine(BiFunction<Delta, AttributeMap, Boolean> predicate, String newLine) {
    final Delta.Iterator it = iterator();
    Delta line = new Delta();
    while (it.hasNext()) {
      if (it.peekType() != INSERT) return;
      final Op thisOp = it.peek();
      final int start = thisOp.length() - it.peekLength();
      final int index =
          thisOp.isTextInsert() ? thisOp.argAsString().indexOf(newLine, start) - start : -1;
      if (index < 0) line.push(it.next());
      else if (index > 0) line.push(it.next(index));
      else {
        if (!predicate.apply(line, it.next(1).attributes())) return;
        line = new Delta();
      }
    }
    if (line.length() > 0) predicate.apply(line, null);
  }

  public void eachLine(BiFunction<Delta, AttributeMap, Boolean> applyFunction) {
    eachLine(applyFunction, "\n");
  }

  public Delta invert(Delta base) {
    final Delta inverted = new Delta();
    reduce(
        0,
        (Integer baseIndex, Op op) -> {
          if (op.isInsert()) inverted.delete(op.length());
          else if (op.isRetain() && op.attributes() == null) {
            inverted.retain(op.length());
            return baseIndex + op.length();
          } else if (op.isDelete() || (op.isRetain() && op.hasAttributes())) {
            int length = op.length();
            final Delta slice = base.slice(baseIndex, baseIndex + length);
            slice.forEach(
                (baseOp) -> {
                  if (op.isDelete()) inverted.push(baseOp);
                  else if (op.isRetain() && op.hasAttributes())
                    inverted.retain(
                        baseOp.length(), AttributeMap.invert(op.attributes(), baseOp.attributes()));
                });
            return baseIndex + length;
          }
          return baseIndex;
        });
    return inverted.chop();
  }

  public Delta slice(int start, int end) {
    final Delta newDelta = new Delta();
    final Delta.Iterator it = iterator();
    int index = 0;
    while (index < end && it.hasNext()) {
      Op nextOp;
      if (index < start) nextOp = it.next(start - index);
      else {
        nextOp = it.next(end - index);
        newDelta.add(nextOp);
      }
      index += nextOp.length();
    }
    return newDelta;
  }

  public Delta concat(Delta other) {
    final Delta delta = new Delta(this);
    if (!other.isEmpty()) {
      delta.push(other.getFirst());
      delta.addAll(other.subList(1, other.size()));
    }
    return delta;
  }

  public String plainText() {
    StringBuilder builder = new StringBuilder();
    for (Op op : this) {
      if (op.isTextInsert()) {
        builder.append(op.argAsString());
      } else {
        builder.append("\n");
      }
    }
    return builder.toString();
  }

  @Override
  public String toString() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
    try {
      return writer.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return "Error while generating json:\n" + e.getMessage();
    }
  }

  public static class Iterator implements java.util.Iterator<Op> {

    private final Delta delta;

    private int index = 0;
    private int offset = 0;

    Iterator(Delta delta) {
      this.delta = delta;
    }

    public Op next(int length) {
      if (index >= delta.size()) return Op.retain(Integer.MAX_VALUE, null);

      final Op nextOp = delta.get(index);
      final int offset = this.offset;
      final int opLength = nextOp.length();

      if (length >= opLength - offset) {
        length = opLength - offset;
        this.index += 1;
        this.offset = 0;
      } else {
        this.offset += length;
      }

      if (nextOp.isDelete()) {
        return Op.delete(length);
      } else {
        Op retOp;
        if (nextOp.isRetain()) retOp = Op.retain(length, nextOp.attributes());
        else if (nextOp.isTextInsert())
          retOp =
              Op.insert(
                  nextOp.argAsString().substring(offset, offset + length), nextOp.attributes());
        else retOp = Op.insert(nextOp.arg(), nextOp.attributes());
        return retOp;
      }
    }

    public Op peek() {
      if (index >= delta.size()) return null;
      return delta.get(index);
    }

    public int peekLength() {
      if (index >= delta.size()) return Integer.MAX_VALUE;
      return delta.get(index).length() - offset;
    }

    public Op.Type peekType() {
      if (index >= delta.size()) return Op.Type.RETAIN;
      return delta.get(index).type();
    }

    public Delta rest() {
      if (!hasNext()) return new Delta();
      if (offset == 0) return new Delta(delta.subList(index, delta.size()));
      final int offset = this.offset;
      final int index = this.index;
      final Op next = next();
      final Delta rest = new Delta(delta.subList(this.index, delta.size()));
      this.offset = offset;
      this.index = index;
      var delta = new Delta(List.of(next));
      delta.addAll(rest);
      return delta;
    }

    @Override
    public boolean hasNext() {
      return this.peekLength() < Integer.MAX_VALUE;
    }

    @Override
    public Op next() {
      return next(Integer.MAX_VALUE);
    }
  }
}
