package org.mantoux.delta;

import java.util.Objects;

import static org.mantoux.delta.Op.Type.*;

public class Op {
  private Object  insert;
  private Integer delete;
  private Integer retain;

  private AttributeMap attributes;

  public boolean isDelete() {
    return type().equals(DELETE);
  }

  public boolean isInsert() {
    return type().equals(Type.INSERT);
  }

  public boolean isStringInsert() {
    return isInsert() && insert instanceof String;
  }

  public boolean isRetain() {
    return type().equals(RETAIN);
  }

  public Type type() {
    if (insert != null)
      return Type.INSERT;
    if (delete != null)
      return DELETE;
    if (retain != null)
      return RETAIN;
    throw new IllegalStateException("Op has no insert, delete or retain");
  }

  public Op copy() {
    switch (type()) {
      case RETAIN:
        return Op.retain(retain, attributes != null ? attributes.copy() : null);
      case DELETE:
        return Op.delete(delete);
      case INSERT:
        return Op.insert(insert, attributes != null ? attributes.copy() : null);
      default:
        throw new IllegalStateException("Op has no insert, delete or retain");
    }
  }

  public int length() {
    if (type().equals(DELETE))
      return delete;
    if (type().equals(RETAIN))
      return retain;
    if (insert instanceof String)
      return ((String) insert).length();
    return 1;
  }

  public AttributeMap attributes() {
    if (type().equals(DELETE))
      return null;
    return attributes != null ? attributes.copy() : null;
  }

  public Object arg() {
    if (type().equals(Type.INSERT))
      return insert;
    throw new UnsupportedOperationException("Only insert op has an argument");
  }

  public String argAsString() {
    if (!(arg() instanceof String))
      throw new ClassCastException("Argument is not of type String");
    return (String) insert;
  }

  public boolean hasAttributes() {
    if (isDelete())
      return false;
    return attributes != null;
  }

  @Override
  public int hashCode() {
    return Objects.hash(insert, delete, retain, attributes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Op op = (Op) o;
    return Objects.equals(insert, op.insert) && Objects.equals(delete, op.delete) && Objects.equals(
      retain,
      op.retain) && Objects.equals(attributes, op.attributes);
  }

  @Override
  public String toString() {
    switch (type()) {
      case RETAIN:
        return "Op: {\n  " + RETAIN.name().toLowerCase() + ": " + retain + "\n}";
      case INSERT:
        return "Op: {\n  " + INSERT.name().toLowerCase() + ": " + insert + "\n}";
      case DELETE:
        return "Op: {\n  " + DELETE.name().toLowerCase() + ": " + delete + "\n}";
    }
    return "Error";
  }

  public enum Type {
    INSERT, DELETE, RETAIN
  }

  static Op retain(int length) {
    return Op.retain(length, null);
  }

  static Op retainUntilEnd() {
    return Op.retain(Integer.MAX_VALUE);
  }

  static Op insert(Object arg) {
    return Op.insert(arg, null);
  }

  static Op retain(int length, AttributeMap attributes) {
    if (length <= 0)
      throw new IllegalArgumentException("Length should be greater than 0");
    Op newOp = new Op();
    if (attributes != null && attributes.size() > 0)
      newOp.attributes = attributes;
    newOp.retain = length;
    return newOp;
  }

  static Op insert(Object arg, AttributeMap attributes) {
    Op newOp = new Op();
    if (attributes != null && attributes.size() > 0)
      newOp.attributes = attributes;
    newOp.insert = arg;
    return newOp;
  }

  static Op delete(int length) {
    if (length <= 0)
      throw new IllegalArgumentException("Length should be greater than 0");
    Op newOp = new Op();
    newOp.delete = length;
    return newOp;
  }
}

