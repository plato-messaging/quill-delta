package com.plato.delta;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AttributeMap extends HashMap<String, Object> {

  public AttributeMap(AttributeMap b) {
    super(b);
  }

  public AttributeMap() {
    super();
  }

  private AttributeMap(Map<String, Object> input) {
    super(input);
  }

  public AttributeMap copy() {
    return new AttributeMap(this);
  }

  static AttributeMap of(String key, Object value) {
    return new AttributeMap(Map.of(key, value));
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
    AttributeMap _a = a == null ? a : new AttributeMap();
    AttributeMap _b = b == null ? b : new AttributeMap();
    AttributeMap attributes = new AttributeMap(_b);
    if (!keepNull)
      attributes.remove(null);

    for (String key : _a.keySet()) {
      if (_a.get(key) != null && !_b.containsKey(key))
        attributes.put(key, _a.get(key));
    }

    return attributes.isEmpty() ? null : new AttributeMap(attributes);
  }

  /**
   * @param a
   * @param b
   * @return
   */
  static AttributeMap diff(AttributeMap a, AttributeMap b) {
    AttributeMap _a = a == null ? a : new AttributeMap();
    AttributeMap _b = b == null ? b : new AttributeMap();
    AttributeMap attributes = new AttributeMap();
    Set<String> keys = _a.keySet();
    keys.addAll(_b.keySet());
    for (String k : keys) {
      if (!_a.get(k).equals(_b.get(k)))
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
    AttributeMap _attr = attr == null ? attr : new AttributeMap();
    AttributeMap _base = base == null ? base : new AttributeMap();
    AttributeMap baseInverted = new AttributeMap();
    for (String k : _base.keySet()) {
      if (!_base.get(k).equals(_attr.get(k)) && _attr.containsKey(k))
        baseInverted.put(k, _base.get(k));
    }
    for (String k : _attr.keySet()) {
      if (!_attr.get(k).equals(_base.get(k)) && !_base.containsKey(k))
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
