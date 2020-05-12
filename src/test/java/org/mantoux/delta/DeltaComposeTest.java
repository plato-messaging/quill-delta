package org.mantoux.delta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mantoux.delta.AttributeMap.of;

@DisplayName("Delta composing")
public class DeltaComposeTest {

  @Test
  public void insertInsert() {
    var a = new Delta().insert("A");
    var b = new Delta().insert("B");
    assertEquals(new Delta().insert("B").insert("A"), a.compose(b));
  }

  @Test
  public void insertRetain() {
    var a = new Delta().insert("A");
    var b = new Delta().retain(1, of("bold", true, "color", "red", "font", null));
    var expected = new Delta().insert("A", of("bold", true, "color", "red"));
    assertEquals(expected, a.compose(b));
  }

  @Test
  public void insertDelete() {
    var a = new Delta().insert("A");
    var b = new Delta().delete(1);
    assertEquals(new Delta(), a.compose(b));
  }

  @Test
  public void deleteInsert() {
    var a = new Delta().delete(1);
    var b = new Delta().insert("B");
    var expected = new Delta().insert("B").delete(1);
    assertEquals(expected, a.compose(b));
  }

  @Test
  public void deleteRetain() {
    var a = new Delta().delete(1);
    var b = new Delta().retain(1, of("bold", true, "color", "red"));
    var expected = new Delta().delete(1).retain(1, of("bold", true, "color", "red"));
    assertEquals(expected, a.compose(b));
  }

  @Test
  public void deleteDelete() {
    var a = new Delta().delete(1);
    var b = new Delta().delete(1);
    assertEquals(new Delta().delete(2), a.compose(b));
  }

  @Test
  public void retainInsert() {
    var a = new Delta().retain(1, of("color", "blue"));
    var b = new Delta().insert("B");
    var expected = new Delta().insert("B").retain(1, of("color", "blue"));
    assertEquals(expected, a.compose(b));
  }

  @Test
  public void retainRetain() {
    var a = new Delta().retain(1, of("color", "blue"));
    var b = new Delta().retain(1, of("bold", true, "color", "red", "font", null));
    var expected = new Delta().retain(1, of("bold", true, "color", "red", "font", null));
    assertEquals(expected, a.compose(b));
  }

  @Test
  public void retainDelete() {
    var a = new Delta().retain(1, of("color", "blue"));
    var b = new Delta().delete(1);
    assertEquals(new Delta().delete(1), a.compose(b));
  }

  @Test
  public void insertInMiddleOfText() {
    var a = new Delta().insert("Hello");
    var b = new Delta().retain(3).insert("X");
    assertEquals(new Delta().insert("HelXlo"), a.compose(b));
  }

  @Test
  public void insertDeleteOrdering() {
    var a = new Delta().insert("Hello");
    var b = new Delta().insert("Hello");
    var insertFirst = new Delta().retain(3).insert("X").delete(1);
    var deleteFirst = new Delta().retain(3).delete(1).insert("X");
    var expected = new Delta().insert("HelXo");
    assertEquals(expected, a.compose(insertFirst));
    assertEquals(expected, a.compose(deleteFirst));
  }

  @Test
  public void insertEmbed() {
    var a = new Delta().insert(1, of("src", "https://plato.mantoux.org"));
    var b = new Delta().retain(1, of("alt", "Plato"));
    var expected = new Delta().insert(1, of("src", "https://plato.mantoux.org", "alt", "Plato"));
    assertEquals(expected, a.compose(b));
  }

  @Test
  public void deleteEntireText() {
    var a = new Delta().retain(4).insert("Hello");
    var b = new Delta().delete(9);
    assertEquals(new Delta().delete(4), a.compose(b));
  }

  @Test
  public void retainMoreThanTextLength() {
    var a = new Delta().insert("Hello");
    var b = new Delta().retain(10);
    assertEquals(a, a.compose(b));
  }

  @Test
  public void retainEmptyEmbed() {
    var a = new Delta().insert(1);
    var b = new Delta().retain(1);
    assertEquals(a, a.compose(b));
  }

  @Test
  public void removeAllAttributes() {
    var a = new Delta().insert("A", of("bold", true));
    var b = new Delta().retain(1, of("bold", null));
    assertEquals(new Delta().insert("A"), a.compose(b));
  }

  @Test
  public void removeAllEmbedAttributes() {
    var a = new Delta().insert(2, of("src", "https://plato.mantoux.org"));
    var b = new Delta().retain(1, of("src", null));
    assertEquals(new Delta().insert(2), a.compose(b));
  }

  @Test
  public void immutability() {
    var attr1 = of("bold", true);
    var attr2 = of("bold", true);
    var a1 = new Delta().insert("Test", attr1);
    var a2 = new Delta().insert("Test", attr2);
    var b1 = new Delta().retain(1, of("color", "red")).delete(2);
    var b2 = new Delta().retain(1, of("color", "red")).delete(2);
    var expected = new Delta().insert("T", of("color", "red", "bold", true)).insert("t", attr1);
    assertEquals(expected, a1.compose(b1));
    assertEquals(a1, a2);
    assertEquals(b1, b2);
    assertEquals(attr1, attr2);
  }

  @Test
  public void retainStartOptimization() {
    var a =
      new Delta().insert("A", of("bold", true)).insert("B").insert("C", of("bold", true)).delete(1);
    var b = new Delta().retain(3).insert("D");
    var expected = new Delta().insert("A", of("bold", true))
                              .insert("B")
                              .insert("C", of("bold", true))
                              .insert("D")
                              .delete(1);
    assertEquals(expected, a.compose(b));
  }

  @Test
  public void retainStartOptimizationSplit() {
    var a = new Delta().insert("A", of("bold", true))
                       .insert("B")
                       .insert("C", of("bold", true))
                       .retain(5)
                       .delete(1);
    var b = new Delta().retain(4).insert("D");
    var expected = new Delta().insert("A", of("bold", true))
                              .insert("B")
                              .insert("C", of("bold", true))
                              .retain(1)
                              .insert("D")
                              .retain(4)
                              .delete(1);
    assertEquals(expected, a.compose(b));
  }

  @Test
  public void retainEndOptimization() {
    var a = new Delta().insert("A", of("bold", true)).insert("B").insert("C", of("bold", true));
    var b = new Delta().delete(1);
    var expected = new Delta().insert("B").insert("C", of("bold", true));
    assertEquals(expected, a.compose(b));
  }

  @Test
  public void retainEndOptimizationJoin() {
    var a = new Delta().insert("A", of("bold", true))
                       .insert("B")
                       .insert("C", of("bold", true))
                       .insert("D")
                       .insert("E", of("bold", true))
                       .insert("F");
    var b = new Delta().retain(1).delete(1);
    var expected = new Delta().insert("AC", of("bold", true))
                              .insert("D")
                              .insert("E", of("bold", true))
                              .insert("F");
    assertEquals(expected, a.compose(b));
  }

}
