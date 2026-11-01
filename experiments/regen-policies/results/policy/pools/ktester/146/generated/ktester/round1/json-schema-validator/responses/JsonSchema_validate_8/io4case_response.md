{
  "testCases": [
    {
      "group": "ExecutionCustomizer Exception Propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }"
            }
          ],
          "expected": "RuntimeException",
          "description": "When the Consumer supplied as executionCustomizer throws an unchecked exception, the validate method must let the exception propagate unchanged."
        }
      ]
    },
    {
      "group": "OutputFormat Exception Propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<String>() { @Override public String format(Set<ValidationMessage> messages, ExecutionContext ctx) { throw new IllegalStateException(\"format failure\"); } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "IllegalStateException",
          "description": "If the OutputFormat implementation throws an exception while formatting the validation messages, the exception should bubble up to the caller."
        }
      ]
    },
    {
      "group": "Primitive Root Node Type Mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.numberNode(42)"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "[\"type mismatch\"]",
          "description": "Schema expects an object but the supplied root node is a numeric primitive; validation must report a type‑mismatch message."
        }
      ]
    },
    {
      "group": "Array Root Node Validation – Success",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3)"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "[]",
          "description": "Schema defines an array of integers with minItems=3. The supplied array meets the constraints, so validation returns an empty collection."
        }
      ]
    },
    {
      "group": "Array Root Node Validation – Failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.arrayNode().add(1)"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "[\"minItems constraint violated\"]",
          "description": "Schema requires at least 3 items; the provided array has only one, so a minItems validation message must be produced."
        }
      ]
    },
    {
      "group": "$ref Resolution (internal definition)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNodeMatchingDefinition"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "[]",
          "description": "Schema contains an internal $ref (e.g., \"#/definitions/address\"). The validator must resolve the reference and validate the instance successfully."
        }
      ]
    },
    {
      "group": "Fail‑Fast Mode via ExecutionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "invalidJsonNodeWithMultipleErrors"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.put(ExecutionContext.KEY_FAIL_FAST, true)"
            }
          ],
          "expected": "[\"first error only\"]",
          "description": "When the execution context is configured with the fail‑fast flag, validation should stop after the first error, resulting in a single ValidationMessage."
        }
      ]
    },
    {
      "group": "Empty Schema (no validators)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "anyJsonNode"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "[]",
          "description": "An empty schema object {} defines no constraints; any JSON document must validate successfully, yielding an empty result set."
        }
      ]
    },
    {
      "group": "Recursive Anchor with Circular $ref – No StackOverflow",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "simpleJsonNode"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "[]",
          "description": "Schema is marked as a recursive anchor and contains a $ref that points to itself. Validation must detect the recursion guard and complete without throwing StackOverflowError."
        }
      ]
    },
    {
      "group": "Schema Resource Root with External $ref",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "[]",
          "description": "When isSchemaResourceRoot() returns true, the validator may need to resolve an external $ref (e.g., a URL). The test supplies a mock external schema that validates the instance, confirming that the flag does not break $ref handling."
        }
      ]
    }
  ]
}