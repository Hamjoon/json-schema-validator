Invalid control character at: line 72 column 70 (char 2862)
{
  "tests": [
    {
      "group": "Non‑union – node type mismatch, side‑effects must NOT be recorded",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext with CollectorContext exposing empty evaluatedItems/evaluatedProperties sets"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing a string (e.g., JsonNodeFactory.instance.textNode(\"abc\"))"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.arrayNode()"
            },
            {
              "parameter": "at",
              "value": "\"/0\""
            }
          ],
          "expected": "Singleton Set containing a ValidationMessage describing the type mismatch (nodeType=\"STRING\", schemaType!=STRING). The CollectorContext’s evaluatedItems set remains empty (no side‑effect).",
          "description": "When the node’s actual type does not match the schema type, validate() should return a ValidationMessage and must NOT add the location to evaluatedItems/evaluatedProperties even if schemaPath ends with \"/type\"."
        }
      ]
    },
    {
      "group": "Non‑union – null node handling (node == null)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext not used in this branch)"
            },
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/missing\""
            }
          ],
          "expected": "Singleton Set containing a ValidationMessage where nodeType is \"NULL\" (as returned by TypeFactory) and schemaType is the configured non‑NULL type (e.g., \"STRING\").",
          "description": "If the supplied node is null, TypeValidator should treat its type as NULL, report a mismatch, and return a ValidationMessage."
        }
      ]
    },
    {
      "group": "Non‑union – schemaPath ends with \"/type\" but rootNode is a value node",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext with CollectorContext exposing empty evaluatedItems/evaluatedProperties"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing a boolean true (JsonNodeFactory.instance.booleanNode(true))"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.booleanNode(false)   // rootNode is a primitive value, not array/object"
            },
            {
              "parameter": "at",
              "value": "\"/flag\""
            }
          ],
          "expected": "Empty Set and both evaluatedItems and evaluatedProperties remain unchanged (no addition).",
          "description": "When schemaPath ends with \"/type\" but the root document is a primitive value, the validator must not add the location to either evaluated set."
        }
      ]
    },
    {
      "group": "Union type handling – Union validator returns empty set",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "any JsonNode (e.g., JsonNodeFactory.instance.textNode(\"any\"))"
            },
            {
              "parameter": "rootNode",
              "value": "any JsonNode (e.g., JsonNodeFactory.instance.objectNode())"
            },
            {
              "parameter": "at",
              "value": "\"/some/path\""
            }
          ],
          "expected": "Empty Set – verify that UnionTypeValidator.validate was invoked once with the same parameters and its empty result is returned unchanged.",
          "description": "When the schema type is UNION and the delegated UnionTypeValidator produces no errors, TypeValidator must forward the empty Set without further processing."
        }
      ]
    },
    {
      "group": "Union type handling – Union validator returns multiple messages",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "any JsonNode (e.g., JsonNodeFactory.instance.arrayNode())"
            },
            {
              "parameter": "rootNode",
              "value": "any JsonNode (e.g., JsonNodeFactory.instance.objectNode())"
            },
            {
              "parameter": "at",
              "value": "\"/union/path\""
            }
          ],
          "expected": "Set containing exactly the ValidationMessages returned by UnionTypeValidator.validate – verify that the same Set instance (or an equal Set) is returned unchanged.",
          "description": "When the schema type is UNION and the UnionTypeValidator returns one or more ValidationMessages, TypeValidator must return that Set verbatim."
        }
      ]
    },
    {
      "group": "Schema type matching – each JsonType supported by TypeValidator",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext not used)"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.textNode(\"hello\")"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/greeting\""
            }
          ],
          "expected": "Empty Set – no validation errors because node type STRING matches schemaType STRING.",
          "description": "Validate that a string node passes when the validator is configured with schemaType=STRING."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext not used)"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.numberNode(123)"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/count\""
            }
          ],
          "expected": "Empty Set – node type INTEGER matches schemaType INTEGER.",
          "description": "Validate that an integer number node passes when schemaType=INTEGER."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext not used)"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.booleanNode(false)"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/active\""
            }
          ],
          "expected": "Empty Set – node type BOOLEAN matches schemaType BOOLEAN.",
          "description": "Validate that a boolean node passes when schemaType=BOOLEAN."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext not used)"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.textNode(\"a\"))"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/list\""
            }
          ],
          "expected": "Empty Set – node type ARRAY matches schemaType ARRAY.",
          "description": "Validate that an array node passes when schemaType=ARRAY."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext not used)"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"key\", \"value\")"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/object\""
            }
          ],
          "expected": "Empty Set – node type OBJECT matches schemaType OBJECT.",
          "description": "Validate that an object node passes when schemaType=OBJECT."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext not used)"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.nullNode()"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/null\""
            }
          ],
          "expected": "Empty Set – node type NULL matches schemaType NULL.",
          "description": "Validate that a null node passes when schemaType=NULL."
        }
      ]
    }
  ]
}