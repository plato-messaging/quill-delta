package org.mantoux.delta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mantoux.delta.AttributeMap.of;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Helpers")
class DeltaHelpersTest {

  @Nested
  public class Concat {

    @Test
    public void emptyDelta() {
      var delta = new Delta().insert("Test");
      var concat = new Delta();
      assertEquals(delta, delta.concat(concat));
    }

    @Test
    public void unmergeable() {
      var delta = new Delta().insert("Test");
      var original = new Delta(delta);
      var concat = new Delta().insert("!", of("bold", true));
      var expected = new Delta().insert("Test").insert("!", of("bold", true));
      assertEquals(expected, delta.concat(concat));
      assertEquals(original, delta);
    }

    @Test
    public void mergeable() {
      var delta = new Delta().insert("Test", of("bold", true));
      var original = new Delta(delta);
      var concat = new Delta().insert("!", of("bold", true)).insert("\n");
      var expected = new Delta().insert("Test!", of("bold", true)).insert("\n");
      assertEquals(expected, delta.concat(concat));
      assertEquals(original, delta);
    }
  }

  @Nested
  public class Chop {

    @Test
    public void retain() {
      var delta = new Delta().insert("Test").retain(4);
      var expected = new Delta().insert("Test");
      assertEquals(expected, delta.chop());
    }

    @Test
    public void insert() {
      var delta = new Delta().insert("Test");
      assertEquals(delta, delta.chop());
    }

    @Test
    public void formattedRetain() {
      var delta = new Delta().insert("Test").retain(4, of("bold", true));
      assertEquals(delta, delta.chop());
    }
  }
}
