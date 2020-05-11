package com.plato.delta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.plato.delta.AttributeMap.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Attributes")
class AttributeMapTest {

  @Nested
  public class Compose {

    AttributeMap attributes = of("bold", true, "color", "red");

    @Test
    public void leftIsNull() {
      assertEquals(attributes, compose(null, attributes));
    }

    @Test
    public void rightIsNull() {
      assertEquals(attributes, compose(attributes, null));
    }

    @Test
    public void bothAreNull() {
      assertNull(compose(null, null));
    }

    @Test
    public void missingElement() {
      assertEquals(of("bold", true, "italic", true, "color", "red"),
                   compose(attributes, of("italic", true)));
    }

    @Test
    public void overrideElement() {
      assertEquals(of("bold", true, "color", "blue"), compose(attributes, of("color", "blue")));
    }

    @Test
    public void removeElement() {
      assertEquals(of("color", "red"), compose(attributes, of("bold", null)));
    }

    @Test
    public void removeAll() {
      assertNull(compose(attributes, of("bold", null, "color", null)));
    }

    @Test
    public void removeMissing() {
      assertEquals(attributes, compose(attributes, of("italic", null)));
    }
  }


  @Nested
  public class Invert {

    @Test
    public void onNull() {
      AttributeMap base = of("bold", true);
      assertEquals(new AttributeMap(), invert(null, base));
    }

    @Test
    public void baseNull() {
      AttributeMap attributes = of("bold", true);
      AttributeMap expected = of("bold", null);
      assertEquals(expected, invert(attributes, null));
    }

    @Test
    public void bothNull() {
      assertEquals(new AttributeMap(), invert(null, null));
    }

    @Test
    public void merge() {
      AttributeMap attributes = of("bold", true);
      AttributeMap base = of("italic", true);
      assertEquals(of("bold", null), invert(attributes, base));
    }

    @Test
    public void revertUnset() {
      AttributeMap attributes = of("bold", null);
      AttributeMap base = of("bold", true);
      assertEquals(of("bold", true), invert(attributes, base));
    }

    @Test
    public void replace() {
      AttributeMap attributes = of("color", "red");
      AttributeMap base = of("color", "blue");
      assertEquals(base, invert(attributes, base));
    }

    @Test
    public void noop() {
      AttributeMap attributes = of("color", "red");
      AttributeMap base = of("color", "red");
      assertEquals(new AttributeMap(), invert(attributes, base));
    }

    @Test
    public void combined() {
      AttributeMap attributes = of("bold", true, "italic", null, "color", "red", "size", "12px");
      AttributeMap base = of("font", "serif", "italic", true, "color", "blue", "size", "12px");
      AttributeMap expected = of("bold", null, "italic", true, "color", "blue");
      assertEquals(expected, invert(attributes, base));
    }
  }


  @Nested
  public class Transform {
    AttributeMap left  = of("bold", true, "color", "red", "font", null);
    AttributeMap right = of("color", "blue", "font", "serif", "italic", true);

    @Test
    public void leftNull() {
      assertEquals(left, transform(null, left, false));
    }

    @Test
    public void rightNull() {
      assertNull(transform(right, null, false));
    }

    @Test
    public void bothNull() {
      assertNull(transform(null, null, false));
    }

    @Test
    public void withPriority() {
      assertEquals(of("italic", true), transform(left, right, true));
    }

    @Test
    public void withoutPriority() {
      assertEquals(right, transform(left, right, false));
    }
  }


  @Nested
  public class Diff {
    AttributeMap format = of("bold", true, "color", "red");

    @Test
    public void leftNull() {
      assertEquals(format, diff(null, format));
    }

    @Test
    public void rightNull() {
      assertEquals(of("bold", null, "color", null), diff(format, null));
    }

    @Test
    public void sameFormat() {
      assertNull(diff(format, format));
    }

    @Test
    public void addFormat() {
      AttributeMap added = of("bold", true, "italic", true, "color", "red");
      assertEquals(of("italic", true), diff(format, added));
    }

    @Test
    public void removeFormat() {
      AttributeMap removed = of("bold", true);
      assertEquals(of("color", null), diff(format, removed));
    }

    @Test
    public void overrideFormat() {
      AttributeMap override = of("bold", true, "color", "blue");
      assertEquals(of("color", "blue"), diff(format, override));
    }
  }

}
