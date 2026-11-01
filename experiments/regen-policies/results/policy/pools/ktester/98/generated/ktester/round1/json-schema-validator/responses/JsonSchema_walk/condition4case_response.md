{
  "group": "walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer) - basic and edge cases",
  "cases": [
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "mockExecutionContext (valid, empty context)"
        },
        {
          "parameter": "node",
          "value": "ObjectNode {\"name\":\"John\"} (valid JSON object)"
        },
        {
          "parameter": "validate",
          "value": true
        },
        {
          "parameter": "executionCustomizer",
          "value": "customizer that adds attribute \"mode\"=\"test\" to the ExecutionContext"
        }
      ],
      "expected": "ValidationResult.success() – no ValidationMessage, and the ExecutionContext contains the attribute added by the customizer",
      "description": "Happy‑path: all arguments are non‑null, validation is enabled and a customizer mutates the context. The method should delegate to walkAtNodeInternal and return a successful ValidationResult."
    },
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
          "parameter": "validate",
          "value": false
        },
        {
          "parameter": "executionCustomizer",
          "value": null
        }
      ],
      "expected": "ValidationResult.success() – validation flag is false, so the schema is not applied and the result is successful",
      "description": "When validate is false the walk method must skip validation logic but still return a successful ValidationResult."
    },
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "mockExecutionContext"
        },
        {
          "parameter": "node",
          "value": "ObjectNode {} (empty object)"
        },
        {
          "parameter": "validate",
          "value": true
        },
        {
          "parameter": "executionCustomizer",
          "value": null
        }
      ],
      "expected": "ValidationResult.success() – customizer is optional; method works without it",
      "description": "Customiser is null – the method must handle a missing customiser gracefully."
    },
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": null
        },
        {
          "parameter": "node",
          "value": "ObjectNode {\"a\":1}"
        },
        {
          "parameter": "validate",
          "value": true
        },
        {
          "parameter": "executionCustomizer",
          "value": "no‑op customizer"
        }
      ],
      "expected": "IllegalArgumentException with message containing \"executionContext must not be null\"",
      "description": "The method should reject a null ExecutionContext early (pre‑condition check)."
    },
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "mockExecutionContext"
        },
        {
          "parameter": "node",
          "value": null
        },
        {
          "parameter": "validate",
          "value": true
        },
        {
          "parameter": "executionCustomizer",
          "value": "no‑op customizer"
        }
      ],
      "expected": "IllegalArgumentException with message containing \"node must not be null\"",
      "description": "The method should reject a null JsonNode argument."
    },
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
          "parameter": "validate",
          "value": true
        },
        {
          "parameter": "executionCustomizer",
          "value": "customizer that throws RuntimeException(\"customiser failure\")"
        }
      ],
      "expected": "RuntimeException with message \"customiser failure\" propagated to the caller",
      "description": "If the ExecutionContextCustomizer throws an exception, walk must not swallow it; the exception should propagate."
    },
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "mockExecutionContext (root context with instanceLocation = \"/\")"
        },
        {
          "parameter": "node",
          "value": "ObjectNode {\"type\":\"string\"}"
        },
        {
          "parameter": "validate",
          "value": true
        },
        {
          "parameter": "executionCustomizer",
          "value": "no‑op customizer"
        }
      ],
      "expected": "ValidationResult.success() – atRoot() returns true, internal path handling for root is exercised",
      "description": "Ensures the branch where `atRoot()` is true (the node is the root of the document) is covered."
    },
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "mockExecutionContext (instanceLocation = \"/properties/name\")"
        },
        {
          "parameter": "node",
          "value": "ObjectNode {\"type\":\"string\"}"
        },
        {
          "parameter": "validate",
          "value": true
        },
        {
          "parameter": "executionCustomizer",
          "value": "no‑op customizer"
        }
      ],
      "expected": "ValidationResult.success() – atRoot() returns false, internal handling for non‑root nodes is exercised",
      "description": "Covers the branch where `atRoot()` is false (node is not the document root)."
    }
  ]
}