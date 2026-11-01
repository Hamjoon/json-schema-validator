{
  "testCases": [
    {
      "group": "Null Parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "format",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Both `format` and `executionCustomizer` are null. The method should fail fast with NPE because a non‑null OutputFormat is required."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "format",
              "value": "DummyOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "`rootNode` is null while a valid OutputFormat is supplied. The method must validate its arguments and throw NPE."
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
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "`format` is null while other arguments are valid. The method should reject a null OutputFormat."
        }
      ]
    },
    {
      "group": "Successful Validation – No Errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"   ,
              "note": "e.g. {\"name\":\"John\",\"age\":30}"
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
          "description": "A JSON document that conforms to the schema. The method should return a successful output (empty validation messages) when no customizer is supplied."
        },
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
              "value": "ctx -> ctx.setFailFast(true)"
            }
          ],
          "expected": "CollectingOutputFormat result containing empty Set<ValidationMessage>",
          "description": "Same valid input but with a customizer that sets fail‑fast mode. Because there are no errors, the result is still empty."
        }
      ]
    },
    {
      "group": "Validation Errors – Branches in Walk/Keyword Listeners",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "invalidJsonNode" ,
              "note": "e.g. {\"age\":\"not-a-number\"}"
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
          "description": "Document violates a type constraint. The method should walk the schema, collect the error and return it via the OutputFormat."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "invalidJsonNodeMissingRequired"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setFailFast(true)"
            }
          ],
          "expected": "CollectingOutputFormat result containing exactly one ValidationMessage (first error only)",
          "description": "When fail‑fast is enabled via the customizer, validation should stop after the first error, exercising the `if (executionContext.isFailFast()) break;` branch."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer – Null vs Consumer vs Interface",
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
          "expected": "CollectingOutputFormat result with default ExecutionContext (no custom flags)",
          "description": "No customizer – verifies that the method creates a default ExecutionContext."
        },
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
              "value": "new DummyExecutionContextCustomizer()"
            }
          ],
          "expected": "CollectingOutputFormat result where ExecutionContext has `setValidateAll(true)` applied",
          "description": "Custom implementation of `ExecutionContextCustomizer` that flips a flag, testing the branch that checks `instanceof ExecutionContextCustomizer` and invokes `customize`."
        },
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
              "value": "ctx -> ctx.setValidateAll(true)"
            }
          ],
          "expected": "CollectingOutputFormat result where ExecutionContext has `validateAll` true",
          "description": "Lambda customizer (Consumer) path – the method overload that accepts `Consumer<ExecutionContext>` should be exercised."
        }
      ]
    },
    {
      "group": "Recursive Anchor Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "jsonNodeReferencingRecursiveAnchor"
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
          "expected": "CollectingOutputFormat result with no StackOverflowError",
          "description": "Schema contains `$recursiveAnchor: true`. The method must detect recursion and avoid infinite loops, exercising the `if (schema.isRecursiveAnchor())` branch."
        }
      ]
    },
    {
      "group": "Keyword Walk Listener Runner – Null vs Non‑null",
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
          "expected": "CollectingOutputFormat result; keywordWalkListenerRunner stays null",
          "description": "When the schema does not define any walk listeners, `keywordWalkListenerRunner` remains null – the branch `if (keywordWalkListenerRunner != null)` is false."
        },
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
              "value": "ctx -> ctx.setWalkListenerRunner(new DefaultKeywordWalkListenerRunner())"
            }
          ],
          "expected": "CollectingOutputFormat result; walk listener runner invoked",
          "description": "Customizer injects a non‑null `WalkListenerRunner`. The method should take the non‑null path and invoke it, covering the `if (keywordWalkListenerRunner != null)` true branch."
        }
      ]
    }
  ]
}