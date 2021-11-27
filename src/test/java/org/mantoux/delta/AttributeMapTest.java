package org.mantoux.delta;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Attributes")
class AttributeMapTest {

  @Nested
  public class Compose {

    AttributeMap attributes = AttributeMap.of("bold", true, "color", "red");

    @Test
    public void leftIsNull() {
      Assertions.assertEquals(attributes, AttributeMap.compose(null, attributes));
    }

    @Test
    public void rightIsNull() {
      Assertions.assertEquals(attributes, AttributeMap.compose(attributes, null));
    }

    @Test
    public void bothAreNull() {
      assertNull(AttributeMap.compose(null, null));
    }

    @Test
    public void missingElement() {
      Assertions.assertEquals(
          AttributeMap.of("bold", true, "italic", true, "color", "red"),
          AttributeMap.compose(attributes, AttributeMap.of("italic", true)));
    }

    @Test
    public void overrideElement() {
      Assertions.assertEquals(
          AttributeMap.of("bold", true, "color", "blue"),
          AttributeMap.compose(attributes, AttributeMap.of("color", "blue")));
    }

    @Test
    public void removeElement() {
      Assertions.assertEquals(
          AttributeMap.of("color", "red"),
          AttributeMap.compose(attributes, AttributeMap.of("bold", null)));
    }

    @Test
    public void removeAll() {
      assertNull(AttributeMap.compose(attributes, AttributeMap.of("bold", null, "color", null)));
    }

    @Test
    public void removeMissing() {
      Assertions.assertEquals(
          attributes, AttributeMap.compose(attributes, AttributeMap.of("italic", null)));
    }
  }

  @Nested
  public class Invert {

    @Test
    public void onNull() {
      AttributeMap base = AttributeMap.of("bold", true);
      Assertions.assertEquals(new AttributeMap(), AttributeMap.invert(null, base));
    }

    @Test
    public void baseNull() {
      AttributeMap attributes = AttributeMap.of("bold", true);
      AttributeMap expected = AttributeMap.of("bold", null);
      Assertions.assertEquals(expected, AttributeMap.invert(attributes, null));
    }

    @Test
    public void bothNull() {
      Assertions.assertEquals(new AttributeMap(), AttributeMap.invert(null, null));
    }

    @Test
    public void merge() {
      AttributeMap attributes = AttributeMap.of("bold", true);
      AttributeMap base = AttributeMap.of("italic", true);
      Assertions.assertEquals(AttributeMap.of("bold", null), AttributeMap.invert(attributes, base));
    }

    @Test
    public void revertUnset() {
      AttributeMap attributes = AttributeMap.of("bold", null);
      AttributeMap base = AttributeMap.of("bold", true);
      Assertions.assertEquals(AttributeMap.of("bold", true), AttributeMap.invert(attributes, base));
    }

    @Test
    public void replace() {
      AttributeMap attributes = AttributeMap.of("color", "red");
      AttributeMap base = AttributeMap.of("color", "blue");
      Assertions.assertEquals(base, AttributeMap.invert(attributes, base));
    }

    @Test
    public void noop() {
      AttributeMap attributes = AttributeMap.of("color", "red");
      AttributeMap base = AttributeMap.of("color", "red");
      Assertions.assertEquals(new AttributeMap(), AttributeMap.invert(attributes, base));
    }

    @Test
    public void combined() {
      AttributeMap attributes =
          AttributeMap.of("bold", true, "italic", null, "color", "red", "size", "12px");
      AttributeMap base =
          AttributeMap.of("font", "serif", "italic", true, "color", "blue", "size", "12px");
      AttributeMap expected = AttributeMap.of("bold", null, "italic", true, "color", "blue");
      Assertions.assertEquals(expected, AttributeMap.invert(attributes, base));
    }
  }

  @Nested
  public class Transform {
    AttributeMap left = AttributeMap.of("bold", true, "color", "red", "font", null);
    AttributeMap right = AttributeMap.of("color", "blue", "font", "serif", "italic", true);

    @Test
    public void leftNull() {
      Assertions.assertEquals(left, AttributeMap.transform(null, left, false));
    }

    @Test
    public void rightNull() {
      assertNull(AttributeMap.transform(right, null, false));
    }

    @Test
    public void bothNull() {
      assertNull(AttributeMap.transform(null, null, false));
    }

    @Test
    public void withPriority() {
      Assertions.assertEquals(
          AttributeMap.of("italic", true), AttributeMap.transform(left, right, true));
    }

    @Test
    public void withoutPriority() {
      Assertions.assertEquals(right, AttributeMap.transform(left, right, false));
    }
  }

  @Nested
  public class Diff {
    AttributeMap format = AttributeMap.of("bold", true, "color", "red");

    @Test
    public void leftNull() {
      Assertions.assertEquals(format, AttributeMap.diff(null, format));
    }

    @Test
    public void rightNull() {
      Assertions.assertEquals(
          AttributeMap.of("bold", null, "color", null), AttributeMap.diff(format, null));
    }

    @Test
    public void sameFormat() {
      assertNull(AttributeMap.diff(format, format));
    }

    @Test
    public void addFormat() {
      AttributeMap added = AttributeMap.of("bold", true, "italic", true, "color", "red");
      Assertions.assertEquals(AttributeMap.of("italic", true), AttributeMap.diff(format, added));
    }

    @Test
    public void removeFormat() {
      AttributeMap removed = AttributeMap.of("bold", true);
      Assertions.assertEquals(AttributeMap.of("color", null), AttributeMap.diff(format, removed));
    }

    @Test
    public void overrideFormat() {
      AttributeMap override = AttributeMap.of("bold", true, "color", "blue");
      Assertions.assertEquals(
          AttributeMap.of("color", "blue"), AttributeMap.diff(format, override));
    }
  }
}
