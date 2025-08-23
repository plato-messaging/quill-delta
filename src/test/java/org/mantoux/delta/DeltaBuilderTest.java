package org.mantoux.delta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mantoux.delta.Op.EMBED;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Delta building")
class DeltaBuilderTest {

  @Nested
  public class Constructor {
    final Delta delta = new Delta();

    @BeforeEach
    public void beforeEach() {
      delta.add(Op.insert("abc"));
      delta.add(Op.retain(1, AttributeMap.of("color", "red")));
      delta.add(Op.delete(4));
      delta.add(Op.insert("def", AttributeMap.of("bold", true)));
      delta.add(Op.retain(6));
    }

    @Test
    public void empty() {
      var delta = new Delta();
      assertNotNull(delta);
      assertEquals(0, delta.size());
    }

    @Test
    public void emptyOps() {
      var delta = new Delta().insert("").delete(0).retain(0);
      assertNotNull(delta);
      assertEquals(0, delta.size());
    }

    @Test
    public void arrayOfOps() {
      var delta = new Delta(this.delta);
      assertEquals(this.delta, delta);
    }

    @Test
    public void delta() {
      var original = new Delta(delta);
      var copy = new Delta(original);
      assertEquals(original, copy);
      assertEquals(delta, copy);
    }
  }

  @Nested
  public class Insert {

    @Test
    public void insertText() {
      var delta = new Delta().insert("test");
      assertEquals(1, delta.size());
      assertEquals(Op.insert("test"), delta.getFirst());
    }

    @Test
    public void insertTextNull() {
      var delta = new Delta().insert("test", null);
      assertEquals(1, delta.size());
      assertEquals(Op.insert("test"), delta.getFirst());
    }

    @Test
    public void insertEmbedded() {
      var delta = new Delta().insert(EMBED);
      assertEquals(1, delta.size());
      assertEquals(Op.insert(EMBED), delta.getFirst());
    }

    @Test
    public void insertEmbeddedWithAttribute() {
      var attribute = AttributeMap.of("url", "https://plato.mantoux.org", "alt", "Plato");
      var delta = new Delta().insert(EMBED, attribute);
      assertEquals(1, delta.size());
      assertEquals(Op.insert(EMBED, attribute), delta.getFirst());
    }

    /*@Test
    public void insertEmbeddedNonInteger() {
      var embed = Map.of("url", "https://plato.mantoux.org");
      var attribute = AttributeMap.of("alt", "Plato");
      var delta = new Delta().insert(embed, attribute);
      assertEquals(1, delta.ops.size());
      assertEquals(Op.insert(embed, attribute), delta.ops.get(0));
    }*/

    @Test
    public void insertTextAttributes() {
      var delta = new Delta().insert("test", AttributeMap.of("bold", true));
      assertEquals(1, delta.size());
      assertEquals(Op.insert("test", AttributeMap.of("bold", true)), delta.getFirst());
    }

    @Test
    public void insertTextAfterDelete() {
      var delta = new Delta().delete(1).insert("a");
      var expected = new Delta().insert("a").delete(1);
      assertEquals(expected, delta);
    }

    @Test
    public void insertTextAfterDeleteWithMerge() {
      var delta = new Delta().insert("a").delete(1).insert("b");
      var expected = new Delta().insert("ab").delete(1);
      assertEquals(expected, delta);
    }

    @Test
    public void insertTextAfterDeleteNoMerge() {
      var delta = new Delta().insert(EMBED).delete(1).insert("a");
      var expected = new Delta().insert(EMBED).insert("a").delete(1);
      assertEquals(expected, delta);
    }

    @Test
    public void insertTextEmptyAttribute() {
      var delta = new Delta().insert("a", new AttributeMap());
      assertEquals(new Delta().insert("a"), delta);
    }
  }

  @Nested
  public class Delete {

    @Test
    public void deletaZero() {
      var delta = new Delta().delete(0);
      assertEquals(0, delta.size());
    }

    @Test
    public void deletePositive() {
      var delta = new Delta().delete(1);
      assertEquals(1, delta.size());
      assertEquals(Op.delete(1), delta.getFirst());
    }
  }

  @Nested
  public class Retain {

    @Test
    public void retainZero() {
      var delta = new Delta().retain(0);
      assertEquals(0, delta.size());
    }

    @Test
    public void retain() {
      var delta = new Delta().retain(2);
      assertEquals(1, delta.size());
      assertEquals(Op.retain(2), delta.getFirst());
    }

    @Test
    public void retainEmptyAttribute() {
      var delta = new Delta().retain(2, new AttributeMap());
      assertEquals(1, delta.size());
      assertEquals(Op.retain(2), delta.getFirst());
    }

    @Test
    public void retainWithAttribute() {
      var delta = new Delta().retain(1, AttributeMap.of("bold", true));
      assertEquals(1, delta.size());
      assertEquals(Op.retain(1, AttributeMap.of("bold", true)), delta.getFirst());
    }

    @Test
    public void retainEmptyAttributeWithDelete() {
      var delta = new Delta().retain(2, new AttributeMap()).delete(1); // Delete prevents chop
      assertEquals(new Delta().retain(2).delete(1), delta);
    }
  }

  @Nested
  public class Push {

    @Test
    public void pushOnEmpty() {
      var delta = new Delta();
      delta.push(Op.insert("test"));
      assertEquals(1, delta.size());
    }

    @Test
    public void pushConsecutiveDelete() {
      var delta = new Delta().delete(2);
      delta.push(Op.delete(3));
      assertEquals(1, delta.size());
      assertEquals(Op.delete(5), delta.getFirst());
    }

    @Test
    public void pushConsecutiveText() {
      var delta = new Delta().insert("a");
      delta.push(Op.insert("b"));
      assertEquals(1, delta.size());
      assertEquals(Op.insert("ab"), delta.getFirst());
    }

    @Test
    public void pushConsecutiveTextWithMatchingAttributes() {
      var delta = new Delta().insert("a", AttributeMap.of("bold", true));
      delta.push(Op.insert("b", AttributeMap.of("bold", true)));
      assertEquals(1, delta.size());
      assertEquals(Op.insert("ab", AttributeMap.of("bold", true)), delta.getFirst());
    }

    @Test
    public void pushConsecutiveRetainsWithMatchingAttributes() {
      var delta = new Delta().retain(1, AttributeMap.of("bold", true));
      delta.push(Op.retain(3, AttributeMap.of("bold", true)));
      assertEquals(1, delta.size());
      assertEquals(Op.retain(4, AttributeMap.of("bold", true)), delta.getFirst());
    }

    @Test
    public void pushConsecutiveTextWithMismatchedAttributes() {
      var delta = new Delta().insert("a", AttributeMap.of("bold", true));
      delta.push(Op.insert("b"));
      assertEquals(2, delta.size());
    }

    @Test
    public void pushConsecutiveRetainsWithMismatchedAttributes() {
      var delta = new Delta().retain(1, AttributeMap.of("bold", true));
      delta.retain(3);
      assertEquals(2, delta.size());
    }

    @Test
    public void pushConsecutiveEmbedsWithMatchingAttributes() {
      var delta = new Delta().insert(EMBED, AttributeMap.of("alt", "Description"));
      delta.push(Op.insert(EMBED, AttributeMap.of("alt", "Some Other Description")));
      assertEquals(2, delta.size());
    }
  }
}
