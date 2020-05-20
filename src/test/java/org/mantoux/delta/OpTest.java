package org.mantoux.delta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mantoux.delta.AttributeMap.of;

@DisplayName("Op")
class OpTest {

  @Nested
  public class Length {
    @Test
    public void lengthDelete() {
      assertEquals(5, Op.delete(5).length());
    }

    @Test
    public void lengthRetain() {
      assertEquals(2, Op.retain(2).length());
    }

    @Test
    public void lengthInsertText() {
      assertEquals(4, Op.insert("text").length());
    }

    @Test
    public void lengthInsertEmbedded() {
      assertEquals(Op.insert(Op.EMBED).length(), 1);
    }
  }


  @Nested
  public class iterator {
    Delta delta;

    @BeforeEach
    public void beforeEach() {
      delta = new Delta().insert("Hello", of("bold", true))
                         .retain(3)
                         .insert("2", of("src", "https://plato.mantoux.org"))
                         .delete(4);
    }

    @Test
    public void hasNext() {
      OpList.Iterator it = delta.ops.iterator();
      assertTrue(it.hasNext());
    }

    @Test
    public void hasNoNext() {
      OpList.Iterator it = new OpList().iterator();
      assertFalse(it.hasNext());
    }

    @Test
    public void peekLengthNoOffset() {
      OpList.Iterator it = delta.ops.iterator();
      assertEquals(5, it.peekLength(), "Incorrect peek length on insert string");
      it.next();
      assertEquals(3, it.peekLength(), "Incorrect peek length on retain");
      it.next();
      assertEquals(1, it.peekLength(), "Incorrect peek length on insert embed");
      it.next();
      assertEquals(4, it.peekLength(), "Incorrect peel length on delete");
    }

    @Test
    public void next() {
      OpList.Iterator it = delta.ops.iterator();
      for (int i = 0; i < delta.ops.size(); i++) {
        assertEquals(delta.ops.get(i), it.next());
      }
      assertEquals(Op.retainUntilEnd(), it.next());
      assertEquals(Op.retainUntilEnd(), it.next(4));
      assertEquals(Op.retainUntilEnd(), it.next());
    }

    @Test
    public void nextLength() {
      OpList.Iterator it = delta.ops.iterator();
      assertEquals(Op.insert("He", of("bold", true)), it.next(2));
      assertEquals(Op.insert("llo", of("bold", true)), it.next(10));
      assertEquals(Op.retain(1), it.next(1));
      assertEquals(Op.retain(2), it.next(2));
    }

    @Test
    public void rest() {
      OpList.Iterator it = delta.ops.iterator();
      it.next(2);
      OpList expected = new OpList();
      expected.add(Op.insert("llo", of("bold", true)));
      expected.add(Op.retain(3));
      expected.add(Op.insert("2", of("src", "https://plato.mantoux.org")));
      expected.add(Op.delete(4));
      assertEquals(expected, it.rest());
      it.next(3);
      new OpList();
      expected.add(Op.retain(3));
      expected.add(Op.insert("2", of("src", "https://plato.mantoux.org")));
      expected.add(Op.delete(4));
      it.next(3);
      it.next(2);
      it.next(4);
      assertEquals(new OpList(), it.rest());
    }
  }
}
