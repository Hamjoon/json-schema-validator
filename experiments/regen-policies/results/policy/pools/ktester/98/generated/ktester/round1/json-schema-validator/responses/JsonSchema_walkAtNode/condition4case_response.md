{
  "testCases": [
    {
      "group": "Successful validation – simple schema",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext with default settings" },
            { "parameter": "node", "value": "JsonNode representing { \"name\": \"John\" }" },
            { "parameter": "rootNode", "value": "same JsonNode as above (root)" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.isValid() == true and no ValidationMessage objects",
          "description": "Walks a node that conforms to a simple schema (type object with required string property \"name\"). The method should return a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Validation failure – type mismatch",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext with default settings" },
            { "parameter": "node", "value": "JsonNode representing { \"age\": 30 } (object missing required \"name\" property)" },
            { "parameter": "rootNode", "value": "same JsonNode as above (root)" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationMessage contains a message about missing required property \"name\"",
          "description": "The node does not satisfy the required property constraint of the schema, causing the walkAtNode method to collect an error."
        }
      ]
    },
    {
      "group": "Validation disabled – should skip validation",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext with default settings" },
            { "parameter": "node", "value": "JsonNode representing { \"any\": 123 } (any JSON)" },
            { "parameter": "rootNode", "value": "same JsonNode as above (root)" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "validate", "value": false }
          ],
          "expected": "ValidationResult.isValid() == true and no ValidationMessage objects (validation skipped)",
          "description": "When the `validate` flag is false the method should bypass all validators and return a successful result."
        }
      ]
    },
    {
      "group": "Null node handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext with default settings" },
            { "parameter": "node", "value": "null" },
            { "parameter": "rootNode", "value": "null" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationMessage contains a message about node being null or missing",
          "description": "The method should detect a null instance node and produce a validation error rather than throwing NPE."
        }
      ]
    },
    {
      "group": "Recursive anchor handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext where JsonSchema.isRecursiveAnchor() returns true" },
            { "parameter": "node", "value": "JsonNode representing a self‑referencing object (e.g., { \"$ref\": \"#\" })" },
            { "parameter": "rootNode", "value": "same JsonNode as above (root)" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.isValid() == true and no infinite recursion occurs (walk terminates correctly)",
          "description": "When the schema is marked as a recursive anchor the walkAtNode method must handle the self‑reference without entering an endless loop."
        }
      ]
    },
    {
      "group": "Exception handling – UnsupportedEncodingException from InputFormat parsing",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext with a custom InputFormat that throws UnsupportedEncodingException when parsing" },
            { "parameter": "node", "value": "JsonNode representing any JSON (won't be used)" },
            { "parameter": "rootNode", "value": "same JsonNode as above" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "walkAtNode propagates a RuntimeException wrapping UnsupportedEncodingException",
          "description": "If an underlying InputFormat fails with UnsupportedEncodingException, walkAtNodeInternal should surface the problem as a runtime exception."
        }
      ]
    },
    {
      "group": "Exception handling – IOException from schema loading",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext where schema retrieval throws IOException" },
            { "parameter": "node", "value": "JsonNode representing any JSON" },
            { "parameter": "rootNode", "value": "same JsonNode as above" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "walkAtNode propagates a RuntimeException wrapping IOException",
          "description": "When the schema cannot be read (e.g., network error), the method should not swallow the IOException but wrap and re‑throw it."
        }
      ]
    },
    {
      "group": "Custom ExecutionContext – consumer modifies context before validation",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext with a mutable flag `skipAll` initially false" },
            { "parameter": "node", "value": "JsonNode representing { \"name\": \"John\" }" },
            { "parameter": "rootNode", "value": "same JsonNode as above (root)" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationMessage indicates validation was skipped because consumer set `skipAll` to true",
          "description": "When a Consumer<ExecutionContext> customizer is supplied (via the overloaded walkAtNodeInternal), it can change the context to disable validation. The test verifies that the change is respected."
        }
      ]
    }
  ]
}