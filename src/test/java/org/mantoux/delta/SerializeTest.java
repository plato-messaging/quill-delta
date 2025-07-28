package org.mantoux.delta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("(De)Serialization")
public class SerializeTest {

  static ObjectMapper mapper = new ObjectMapper();

  @Test
  public void deserializeInsertEmbed() throws Exception {
    var json =
        """
            {
              "ops": [
                {
                  "retain": 1033,
                  "attributes": {
                    "bold": true,
                    "italic": false
                  }
                },
                {
                  "insert": {
                    "_type": "hr",
                    "_inline": false
                  }
                },
                {
                  "delete": 1
                }
              ]
            }
            """;
    var act = mapper.readValue(json, Delta.class);
    var exp =
        new Delta()
            .retain(1033, AttributeMap.of("bold", true, "italic", false))
            .insert(Map.of("_type", "hr", "_inline", false))
            .delete(1);
    assertEquals(exp, act);
  }

  @Test
  public void deserializeInsertFormattedString() throws Exception {
    var json =
        """
            {
              "ops": [
                {
                  "retain": 1033,
                  "attributes": {
                    "bold": true,
                    "italic": false
                  }
                },
                {
                  "insert": "coucou",
                  "attributes": {
                    "bold": true,
                    "italic": false
                  }
                },
                {
                  "delete": 1
                }
              ]
            }
            """;
    var act = mapper.readValue(json, Delta.class);
    var exp =
        new Delta()
            .retain(1033, AttributeMap.of("bold", true, "italic", false))
            .insert("coucou", AttributeMap.of("bold", true, "italic", false))
            .delete(1);
    assertEquals(act, exp);
  }

  @Test
  public void deserializeInsertBasicString() throws Exception {
    var json =
        """
            {
              "ops": [
                {
                  "retain": 1033
                },
                {
                  "insert": "coucou"
                },
                {
                  "delete": 1
                }
              ]
            }
            """;
    var act = mapper.readValue(json, Delta.class);
    var exp = new Delta().retain(1033).insert("coucou").delete(1);
    assertEquals(act, exp);
  }

  @Test
  void deserializeEmptyDelta() throws Exception {
    var json =
        """
        {
          "ops": []
        }
        """;
    var act = mapper.readValue(json, Delta.class);
    var exp = new Delta();
    assertEquals(act, exp);
  }
}
