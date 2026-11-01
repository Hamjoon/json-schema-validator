{
  "testCases": [
    {
      "group": "Array node edge cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ArrayNode [ \"x\", \"y\", \"z\" ]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "-1"
            }
          ],
          "expected": "null",
          "description": "Negative integer index on an ArrayNode should be considered out of range and return null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ArrayNode [ \"x\", \"y\", \"z\" ]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\"-2\""
            }
          ],
          "expected": "null",
          "description": "String representing a negative integer is not a valid array index; getNode must return null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ArrayNode [ \"a\", \"b\", \"c\" ]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "2L"
            }
          ],
          "expected": "JsonNode with text value \"c\"",
          "description": "A Long index within bounds should be accepted and return the element at that position."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ArrayNode [ \"a\", \"b\", \"c\" ]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "java.math.BigInteger.valueOf(1)"
            }
          ],
          "expected": "JsonNode with text value \"b\"",
          "description": "BigInteger that represents a valid integer index should be treated like an int and return the correct element."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ArrayNode [ \"a\", \"b\", \"c\" ]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "1.0d"
            }
          ],
          "expected": "null",
          "description": "A Double that is numerically integral is *not* considered an integer index by JsonNodes.get; the method should return null."
        }
      ]
    },
    {
      "group": "Object node non‑string keys",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode { \"key\": \"value\" }"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "0"
            }
          ],
          "expected": "null",
          "description": "When the node is an ObjectNode, a numeric propertyOrIndex is not a valid field name and must yield null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode { \"true\": false }"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "true"
            }
          ],
          "expected": "null",
          "description": "Boolean propertyOrIndex on an ObjectNode is not a legal property name; getNode should return null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode { \"empty\": \"value\" }"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\"\""
            }
          ],
          "expected": "null",
          "description": "An empty string is not a defined property; the method must return null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode { \" name \": \"value\" }"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\" name \""
            }
          ],
          "expected": "JsonNode with text value \"value\"",
          "description": "Property names with surrounding spaces are valid keys; the exact string must match to retrieve the value."
        }
      ]
    },
    {
      "group": "Special node types",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "NullNode"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\"any\""
            }
          ],
          "expected": "null",
          "description": "A NullNode has no children; any propertyOrIndex should result in null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "MissingNode"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\"any\""
            }
          ],
          "expected": "null",
          "description": "MissingNode represents an absent value; getNode must safely return null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "BooleanNode true"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "\"any\""
            }
          ],
          "expected": "null",
          "description": "Scalar nodes (e.g., BooleanNode) cannot be traversed; the method should return null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode { \"inner\": \"val\" }"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "TextNode \"inner\""
            }
          ],
          "expected": "null",
          "description": "When propertyOrIndex itself is a JsonNode (instead of a plain String/Number), JsonNodes.get does not treat it as a key; expected result is null."
        }
      ]
    },
    {
      "group": "Array node with non‑integer numeric types",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ArrayNode [ \"a\", \"b\", \"c\" ]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "1.5f"
            }
          ],
          "expected": "null",
          "description": "A Float that is not an integer should not be accepted as an array index; getNode must return null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ArrayNode [ \"first\", \"second\" ]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "0L"
            }
          ],
          "expected": "JsonNode with text value \"first\"",
          "description": "Long zero is a valid array index; the method should retrieve the first element."
        }
      ]
    }
  ]
}