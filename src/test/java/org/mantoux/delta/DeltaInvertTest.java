package org.mantoux.delta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mantoux.delta.AttributeMap.of;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Delta inverting")
public class DeltaInvertTest {

  @Test
  public void insert() {
    var delta = new Delta().retain(2).insert("A");
    var base = new Delta().insert("123456");
    var expected = new Delta().retain(2).delete(1);
    var inverted = delta.invert(base);
    assertEquals(expected, inverted);
    assertEquals(base, base.compose(delta).compose(inverted));
  }

  @Test
  public void delete() {
    var delta = new Delta().retain(2).delete(3);
    var base = new Delta().insert("123456");
    var expected = new Delta().retain(2).insert("345");
    var inverted = delta.invert(base);
    assertEquals(expected, inverted);
    assertEquals(base, base.compose(delta).compose(inverted));
  }

  @Test
  public void retain() {
    var delta = new Delta().retain(2).retain(3, of("bold", true));
    var base = new Delta().insert("123456");
    var expected = new Delta().retain(2).retain(3, of("bold", null));
    var inverted = delta.invert(base);
    assertEquals(expected, inverted);
    assertEquals(base, base.compose(delta).compose(inverted));
  }

  @Test
  public void retainOnDeltaWithDifferentAttributes() {
    var delta = new Delta().retain(4, of("italic", true));
    var base = new Delta().insert("123").insert("4", of("bold", true));
    var expected = new Delta().retain(4, of("italic", null));
    var inverted = delta.invert(base);
    assertEquals(expected, inverted);
    assertEquals(base, base.compose(delta).compose(inverted));
  }

  @Test
  public void combined() {
    var delta =
        new Delta()
            .retain(2)
            .delete(2)
            .insert("AB", of("italic", true))
            .retain(2, of("italic", null, "bold", true))
            .retain(2, of("color", "red"))
            .delete(1);
    var base =
        new Delta()
            .insert("123", of("bold", true))
            .insert("456", of("italic", true))
            .insert("789", of("color", "red", "bold", true));
    var expected =
        new Delta()
            .retain(2)
            .insert("3", of("bold", true))
            .insert("4", of("italic", true))
            .delete(2)
            .retain(2, of("italic", true, "bold", null))
            .retain(2)
            .insert("9", of("color", "red", "bold", true));
    var inverted = delta.invert(base);
    assertEquals(expected, inverted);
    assertEquals(base, base.compose(delta).compose(inverted));
  }
}
