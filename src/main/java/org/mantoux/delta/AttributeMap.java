package org.mantoux.delta;

import java.util.*;

public class AttributeMap extends HashMap<String, Object> {

  public AttributeMap(AttributeMap b) {
    super(b);
  }

  public AttributeMap() {
    super();
  }

  public AttributeMap(Map<String, Object> input) {
    super(input);
  }

  public static AttributeMap of(String key, Object value) {
    Map<String, Object> temp = new HashMap<>();
    temp.put(key, value);
    return new AttributeMap(temp);
  }

  public static AttributeMap of(String key0, Object value0, String key1, Object value1) {
    Map<String, Object> temp = new HashMap<>();
    temp.put(key0, value0);
    temp.put(key1, value1);
    return new AttributeMap(temp);
  }

  public static AttributeMap of(String key0,
                                Object value0,
                                String key1,
                                Object value1,
                                String key2,
                                Object value2) {
    Map<String, Object> temp = new HashMap<>();
    temp.put(key0, value0);
    temp.put(key1, value1);
    temp.put(key2, value2);
    return new AttributeMap(temp);
  }

  public static AttributeMap of(String key0,
                                Object value0,
                                String key1,
                                Object value1,
                                String key2,
                                Object value2,
                                String key3,
                                Object value3) {
    Map<String, Object> temp = new HashMap<>();
    temp.put(key0, value0);
    temp.put(key1, value1);
    temp.put(key2, value2);
    temp.put(key3, value3);
    return new AttributeMap(temp);
  }

  public AttributeMap copy() {
    return new AttributeMap(this);
  }

  /**
   * Union of attributes, where conflict are overriden by second argument
   *
   * @param a        an attribute map
   * @param b        an attribute map
   * @param keepNull if inputB has {@code null} key, keep it
   * @return the composed attribute map
   */
  static AttributeMap compose(AttributeMap a, AttributeMap b, boolean keepNull) {
    AttributeMap _a = a != null ? a : new AttributeMap();
    AttributeMap _b = b != null ? b : new AttributeMap();
    AttributeMap attributes = new AttributeMap(_b);
    if (!keepNull) {
      Set<String> keysToRemove = new HashSet<>();
      attributes.forEach((key, value) -> {
        if (value == null)
          keysToRemove.add(key);
      });
      keysToRemove.forEach(attributes::remove);
    }

    for (String key : _a.keySet()) {
      if (_a.get(key) != null && !_b.containsKey(key))
        attributes.put(key, _a.get(key));
    }

    return attributes.isEmpty() ? null : new AttributeMap(attributes);
  }

  static AttributeMap compose(AttributeMap a, AttributeMap b) {
    return compose(a, b, false);
  }

  /**
   * @param a
   * @param b
   * @return
   */
  static AttributeMap diff(AttributeMap a, AttributeMap b) {
    AttributeMap _a = a != null ? a : new AttributeMap();
    AttributeMap _b = b != null ? b : new AttributeMap();
    AttributeMap attributes = new AttributeMap();
    Set<String> keys = new HashSet<>(_a.keySet());
    keys.addAll(_b.keySet());
    for (String k : keys) {
      if (!Objects.equals(_a.get(k), _b.get(k)))
        attributes.put(k, _b.get(k));
    }
    return attributes.isEmpty() ? null : attributes;
  }

  /**
   * @param attr
   * @param base
   * @return
   */
  static AttributeMap invert(AttributeMap attr, AttributeMap base) {
    AttributeMap _attr = attr != null ? attr : new AttributeMap();
    AttributeMap _base = base != null ? base : new AttributeMap();
    AttributeMap baseInverted = new AttributeMap();
    for (String k : _base.keySet()) {
      if (!_base.get(k).equals(_attr.get(k)) && _attr.containsKey(k))
        baseInverted.put(k, _base.get(k));
    }
    for (String k : _attr.keySet()) {
      if (!Objects.equals(_attr.get(k), _base.get(k)) && !_base.containsKey(k))
        baseInverted.put(k, null);
    }
    return baseInverted;
  }

  static AttributeMap transform(AttributeMap a, AttributeMap b) {
    return transform(a, b, false);
  }

  static AttributeMap transform(AttributeMap a, AttributeMap b, boolean priority) {
    if (a == null)
      return b;
    if (b == null)
      return null;
    if (!priority)
      return b; // b simply overwrites us without priority
    AttributeMap attributes = new AttributeMap();
    for (String k : b.keySet()) {
      if (!a.containsKey(k))
        attributes.put(k, b.get(k));
    }
    return attributes.isEmpty() ? null : attributes;
  }

}
