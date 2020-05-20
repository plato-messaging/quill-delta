package org.mantoux.delta;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static org.mantoux.delta.Op.Type.DELETE;
import static org.mantoux.delta.Op.Type.RETAIN;

@JsonInclude(value = NON_EMPTY)
public class Op {

  // 0 length white space
  static final String EMBED = String.valueOf((char) 0x200b);

  @JsonProperty()
  private String  insert;
  @JsonProperty()
  private Integer delete;
  @JsonProperty()
  private Integer retain;

  @JsonProperty()
  private AttributeMap attributes;

  @JsonIgnore
  public boolean isDelete() {
    return type().equals(DELETE);
  }

  @JsonIgnore
  public boolean isInsert() {
    return type().equals(Type.INSERT);
  }

  @JsonIgnore
  public boolean isTextInsert() {
    return isInsert() && !EMBED.equals(insert);
  }

  @JsonIgnore
  public boolean isRetain() {
    return type().equals(RETAIN);
  }

  public static Op insert(String arg) {
    return Op.insert(arg, null);
  }

  public static Op insert(String arg, AttributeMap attributes) {
    Op newOp = new Op();
    if (attributes != null && attributes.size() > 0)
      newOp.attributes = attributes;
    newOp.insert = arg;
    return newOp;
  }

  public static Op retain(int length) {
    return Op.retain(length, null);
  }

  public static Op retain(int length, AttributeMap attributes) {
    if (length <= 0)
      throw new IllegalArgumentException("Length should be greater than 0");
    Op newOp = new Op();
    if (attributes != null && attributes.size() > 0)
      newOp.attributes = attributes;
    newOp.retain = length;
    return newOp;
  }

  public static Op delete(int length) {
    if (length <= 0)
      throw new IllegalArgumentException("Length should be greater than 0");
    Op newOp = new Op();
    newOp.delete = length;
    return newOp;
  }

  @JsonGetter("type")
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
    return insert.length();
  }

  public AttributeMap attributes() {
    if (type().equals(DELETE))
      return null;
    return attributes != null ? attributes.copy() : null;
  }

  public String arg() {
    if (Type.INSERT.equals(type()))
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
    ObjectMapper mapper = new ObjectMapper();
    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
    try {
      return writer.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return "Error while generating json:\n" + e.getMessage();
    }
  }

  public enum Type {
    INSERT, DELETE, RETAIN
  }

  static Op retainUntilEnd() {
    return Op.retain(Integer.MAX_VALUE);
  }
}

