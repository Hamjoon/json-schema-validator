{
  "testCases": [
    {
      "group": "Null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "NullPointerException",
          "description": "validate should throw NPE when the JSON document (rootNode) is null."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "format",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "validate should reject a null OutputFormat and throw IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "SimpleStringOutputFormat result containing an empty validation set",
          "description": "When executionCustomizer is null the method should use a default no‑op customizer and return a successful validation result (empty set)."
        }
      ]
    },
    {
      "group": "Successful validation – no errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode().put(\"name\", \"John\")"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "String \"[]\" (empty JSON array) – meaning no ValidationMessage objects",
          "description": "A JSON document that satisfies the schema should produce an empty validation result, formatted by SimpleStringOutputFormat."
        }
      ]
    },
    {
      "group": "Validation with errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode().put(\"age\", \"twenty\")"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "String containing at least one ValidationMessage (e.g., \"age: integer expected\")",
          "description": "When the JSON does not conform to the schema (wrong type for \"age\"), the validator should return a non‑empty set, which the OutputFormat turns into a descriptive string."
        }
      ]
    },
    {
      "group": "Custom ExecutionContext – flag manipulation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode().put(\"id\", 1)"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"skip\", true)"
            }
          ],
          "expected": "String \"[]\" (empty validation set) because the customizer signals the walk to skip validation",
          "description": "A customizer that sets a known attribute (e.g., \"skip\") which the internal walk logic checks to bypass validation, resulting in an empty result."
        }
      ]
    },
    {
      "group": "Custom ExecutionContext – exception propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode().put(\"name\", \"Alice\")"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }"
            }
          ],
          "expected": "RuntimeException with message \"customizer failure\"",
          "description": "If the ExecutionContextCustomizer throws an exception, validate should propagate it unchanged."
        }
      ]
    },
    {
      "group": "Recursive anchor handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode().put(\"self\", objectNode())"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setRecursiveAnchor(true)"
            }
          ],
          "expected": "String representing successful validation (empty array) – recursive anchor should be honoured without infinite recursion",
          "description": "When the schema is marked as a recursive anchor, the customizer enables that flag; the validator must handle it correctly and still terminate, returning an empty result for a valid document."
        }
      ]
    }
  ]
}