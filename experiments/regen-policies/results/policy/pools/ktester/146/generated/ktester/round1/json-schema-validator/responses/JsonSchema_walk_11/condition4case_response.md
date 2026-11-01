{
  "testCases": [
    {
      "group": "Validate flag true – normal flow",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode {\"name\":\"John\"}"
            },
            {
              "parameter": "outputFormat",
              "value": "mockOutputFormatReturningString"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "\"validation passed\"",
          "description": "When `validate` is true and no customizer is supplied, the method should run all validators on the supplied node and return the value produced by the OutputFormat (here a string indicating success)."
        }
      ]
    },
    {
      "group": "Validate flag false – skip validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ArrayNode [1,2,3]"
            },
            {
              "parameter": "outputFormat",
              "value": "mockOutputFormatReturningString"
            },
            {
              "parameter": "validate",
              "value": false
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "\"validation skipped\"",
          "description": "When `validate` is false the method must bypass all schema validators and directly invoke the OutputFormat, resulting in a different output that signals validation was skipped."
        }
      ]
    },
    {
      "group": "Non‑null executionCustomizer – should be invoked",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode {\"age\":30}"
            },
            {
              "parameter": "outputFormat",
              "value": "mockOutputFormatReturningString"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "mockConsumerThatAddsFlag"
            }
          ],
          "expected": "\"customizer applied\"",
          "description": "A non‑null `Consumer<ExecutionContext>` must be called exactly once before validation starts. The mock consumer sets a flag in the ExecutionContext; the OutputFormat can read that flag and return a distinct string."
        }
      ]
    },
    {
      "group": "Null node – illegal argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "outputFormat",
              "value": "mockOutputFormatReturningString"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "If the supplied `JsonNode` is null the method should throw an `IllegalArgumentException` because there is no instance to walk."
        }
      ]
    },
    {
      "group": "Null outputFormat – illegal argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode {\"id\":\"123\"}"
            },
            {
              "parameter": "outputFormat",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "A null `OutputFormat` cannot be used to produce a result; the method should throw a `NullPointerException`."
        }
      ]
    },
    {
      "group": "ExecutionContext customizer – modifies context before walk",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode {\"type\":\"string\"}"
            },
            {
              "parameter": "outputFormat",
              "value": "mockOutputFormatReturningString"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "mockConsumerThatAddsMetadata"
            }
          ],
          "expected": "\"metadata added\"",
          "description": "When an `ExecutionContextCustomizer` (a Consumer) is supplied, it must be executed before any validation. The mock consumer adds a metadata entry to the context; the OutputFormat reads this entry and returns a string confirming the addition."
        }
      ]
    }
  ]
}