{
  "testCases": [
    {
      "group": "OutputFormat Exception Propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode",
              "note": "e.g. {\"name\":\"John\"}"
            },
            {
              "parameter": "format",
              "value": "ExceptionThrowingOutputFormat.INSTANCE",
              "note": "OutputFormat that throws RuntimeException when apply() is called"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "RuntimeException",
          "description": "When the supplied OutputFormat throws an exception during result creation, the validate method must let the exception propagate unchanged."
        }
      ]
    },
    {
      "group": "Custom OutputFormat Generic Return",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode",
              "note": "conforms to the schema"
            },
            {
              "parameter": "format",
              "value": "ErrorCountOutputFormat.INSTANCE",
              "note": "OutputFormat<Integer> that returns the number of validation messages"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "0",
          "description": "A custom OutputFormat that converts the Set<ValidationMessage> into an Integer count. For a valid document the method should return 0."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "invalidJsonNodeMissingRequired",
              "note": "e.g. {} missing required property"
            },
            {
              "parameter": "format",
              "value": "ErrorCountOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "1",
          "description": "The same OutputFormat should return the exact number of validation errors (1 in this case)."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer Throws Exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new IllegalStateException(\"customizer failure\"); }"
            }
          ],
          "expected": "IllegalStateException",
          "description": "If the provided ExecutionContextCustomizer throws an exception, validate must propagate that exception without swallowing it."
        }
      ]
    },
    {
      "group": "Schema $ref Resolution",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "refValidJsonNode",
              "note": "e.g. {\"id\":1,\"name\":\"Alice\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "CollectingOutputFormat result containing empty Set<ValidationMessage>",
          "description": "The schema contains a $ref to an external definition. The validator must resolve the reference and validate the instance successfully."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "refInvalidJsonNode",
              "note": "e.g. {\"id\":\"not-an-int\",\"name\":\"Bob\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "CollectingOutputFormat result containing at least one ValidationMessage",
          "description": "When the $ref target schema is violated, validation errors must be reported correctly."
        }
      ]
    },
    {
      "group": "Array Node Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validArrayNode",
              "note": "[1,2,3]"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "CollectingOutputFormat result containing empty Set<ValidationMessage>",
          "description": "Schema expects an array of integers. A correct array should validate with no errors."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "invalidArrayNode",
              "note": "[1,\"two\",3]"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "CollectingOutputFormat result containing at least one ValidationMessage",
          "description": "An array containing a non‑integer element violates the schema; the validator must report the type error."
        }
      ]
    },
    {
      "group": "KeywordWalkListenerRunner Pre‑set in Schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "CollectingOutputFormat result; WalkListenerRunner invoked (its side‑effect recorded)",
          "description": "The JsonSchema instance already has `keywordWalkListenerRunner` set to a mock runner that records invocation. The method should use this runner without needing a customizer."
        }
      ]
    },
    {
      "group": "Multiple Customizer Flags – FailFast Overrides ValidateAll",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "invalidJsonNodeMultipleErrors",
              "note": "{\"age\":\"string\",\"name\":null}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { ctx.setValidateAll(true); ctx.setFailFast(true); }"
            }
          ],
          "expected": "CollectingOutputFormat result containing exactly one ValidationMessage",
          "description": "When both `validateAll` and `failFast` are true, failFast takes precedence and validation stops after the first error."
        }
      ]
    },
    {
      "group": "Empty Object with Required Property",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "emptyObjectNode",
              "note": "{}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "CollectingOutputFormat result containing exactly one ValidationMessage about missing required property",
          "description": "Schema defines a required property `name`. An empty object should trigger a single required‑property validation error."
        }
      ]
    }
  ]
}