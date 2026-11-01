{
  "testCases": [
    {
      "group": "Object node property access",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode { \"name\": \"Alice\", \"age\": 30 }"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\"name\""
            }
          ],
          "expected": "JsonNode with text value \"Alice\"",
          "description": "When the node is an ObjectNode and the property name exists, getNode should return the corresponding child JsonNode."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode { \"name\": \"Alice\", \"age\": 30 }"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\"missing\""
            }
          ],
          "expected": "null",
          "description": "When the node is an ObjectNode but the property does not exist, getNode should return null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode { \"nullValue\": null }"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\"nullValue\""
            }
          ],
          "expected": "null JsonNode (Jackson NullNode)",
          "description": "When the property exists but its value is JSON null, getNode should return a NullNode (treated as non‑null object but representing null)."
        }
      ]
    },
    {
      "group": "Array node index access",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ArrayNode [ \"a\", \"b\", \"c\" ]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "1"
            }
          ],
          "expected": "JsonNode with text value \"b\"",
          "description": "When the node is an ArrayNode and a valid integer index is supplied, getNode should return the element at that index."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ArrayNode [ \"a\", \"b\", \"c\" ]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "5"
            }
          ],
          "expected": "null",
          "description": "When the index is out of bounds, getNode should return null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ArrayNode [ \"a\", \"b\", \"c\" ]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\"2\""
            }
          ],
          "expected": "JsonNode with text value \"c\"",
          "description": "When the propertyOrIndex is a String that can be parsed as an integer, getNode should treat it as an array index and return the element."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ArrayNode [ \"a\", \"b\", \"c\" ]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\"invalid\""
            }
          ],
          "expected": "null",
          "description": "When the propertyOrIndex is a non‑numeric String for an ArrayNode, getNode should return null."
        }
      ]
    },
    {
      "group": "Invalid or edge inputs",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\"any\""
            }
          ],
          "expected": "null",
          "description": "If the supplied JsonNode is null, getNode should safely return null without throwing."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode { \"foo\": \"bar\" }"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "null"
            }
          ],
          "expected": "null",
          "description": "If propertyOrIndex is null, getNode should return null regardless of node type."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "TextNode \"plain string\""
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\"any\""
            }
          ],
          "expected": "null",
          "description": "When the node is a scalar (non‑container) type, getNode should return null because there are no child properties or indices."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode { \"num\": 42 }"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "42.0"
            }
          ],
          "expected": "null",
          "description": "When propertyOrIndex is a non‑integer numeric type (e.g., Double), getNode should not treat it as an array index and return null."
        }
      ]
    }
  ]
}