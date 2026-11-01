Invalid control character at: line 10 column 47 (char 258)
{
  "testCases": [
    {
      "group": "Validate flag true with non‑null ExecutionContextCustomizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext() // default context"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"name\", \"John\")"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\", true)"
            }
          ],
          "expected": "CollectingOutputFormat returns an empty Set (no validation errors) and the customizer is applied (ctx.getAttribute(\"custom\") == true)",
          "description": "When validation is enabled and a non‑null ExecutionContextCustomizer is supplied, the schema should be walked, validators executed and the customizer invoked without throwing an exception."
        }
      ]
    },
    {
      "group": "Validate flag false with non‑null ExecutionContextCustomizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"age\", 30)"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": "false"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\", true)"
            }
          ],
          "expected": "CollectingOutputFormat returns an empty Set (no validation performed) and the customizer is still applied",
          "description": "When validation is disabled the walk should skip all validators but still run the supplied customizer."
        }
      ]
    },
    {
      "group": "Validate flag true with null ExecutionContextCustomizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"email\", \"john@example.com\")"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "CollectingOutputFormat returns an empty Set (no validation errors) and no customizer is invoked",
          "description": "The method must correctly handle a null ExecutionContextCustomizer – it should simply skip the customisation step."
        }
      ]
    },
    {
      "group": "Null JsonNode argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) is thrown",
          "description": "The walk method should defensively reject a null JsonNode because it cannot evaluate a schema against a non‑existent instance."
        }
      ]
    },
    {
      "group": "Null ExecutionContext argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"id\", 1)"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "CollectingOutputFormat returns a Set (validation runs with a newly created default ExecutionContext)",
          "description": "If the supplied ExecutionContext is null, the method should create a default context internally and continue processing."
        }
      ]
    },
    {
      "group": "OutputFormat that throws an exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"string\")"
            },
            {
              "parameter": "outputFormat",
              "value": "new ThrowingOutputFormat<Set<ValidationMessage>>() // throws RuntimeException in format()"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "RuntimeException propagated from OutputFormat",
          "description": "When the OutputFormat implementation fails, the walk method must not swallow the exception; it should be propagated to the caller."
        }
      ]
    },
    {
      "group": "Schema with recursive anchor (isRecursiveAnchor = true)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"self\", \"#\")"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "CollectingOutputFormat returns an empty Set and no StackOverflowError occurs",
          "description": "If the schema declares a recursive anchor, walk must recognise it and avoid infinite recursion."
        }
      ]
    },
    {
      "group": "Schema resource root flag (isSchemaResourceRoot = true)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"$id\", \"http://example.com/schema\")"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "CollectingOutputFormat returns a Set of messages that may include $id resolution errors but the walk completes without treating the node as a sub‑schema",
          "description": "When the schema is marked as a resource root, walk should treat the node as a top‑level schema and not attempt to resolve relative references beyond the root."
        }
      ]
    }
  ]
}