{
  "testCases": [
    {
      "group": "Validate when shouldValidateSchema is true and node is non‑null",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "nonNullJsonNode" },
            { "parameter": "rootNode", "value": "nonNullJsonNode" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.of(\"/field\")" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "validate is invoked and its returned Set<ValidationMessage> (e.g., [{\"type\":\"anyOf\",\"message\":\"validation failed\"}]) is returned; schema.walk is NOT invoked",
          "description": "When validation is required and the node exists, AnyOfValidator should delegate to its validate method and return whatever validate returns."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "nonNullJsonNode" },
            { "parameter": "rootNode", "value": "nonNullJsonNode" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.of(\"/field\")" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "validate returns an empty Set; walk returns an empty Set as well",
          "description": "Ensures that a successful validation (empty message set) is propagated unchanged."
        }
      ]
    },
    {
      "group": "Skip validation when shouldValidateSchema is false",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "nonNullJsonNode" },
            { "parameter": "rootNode", "value": "nonNullJsonNode" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.of(\"/field\")" },
            { "parameter": "shouldValidateSchema", "value": false }
          ],
          "expected": "validate is NOT invoked; each schema in the schemas list has its walk(executionContext, node, rootNode, instanceLocation, false) called; method returns Collections.emptySet()",
          "description": "When validation is disabled, AnyOfValidator must walk each contained schema with shouldValidateSchema = false and return an empty result set."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "null" },
            { "parameter": "rootNode", "value": "null" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.of(\"/field\")" },
            { "parameter": "shouldValidateSchema", "value": false }
          ],
          "expected": "validate is NOT invoked; schemas (if any) are walked with shouldValidateSchema = false; method returns empty Set",
          "description": "Even when the node is null, disabling validation should cause only schema walking, not validation."
        }
      ]
    },
    {
      "group": "Node is null while shouldValidateSchema is true",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "null" },
            { "parameter": "rootNode", "value": "null" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.of(\"/field\")" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "validate is NOT invoked because (node != null) is false; each schema.walk is called with shouldValidateSchema = false; method returns empty Set",
          "description": "The guard requires both shouldValidateSchema and a non‑null node; if node is null the method falls back to schema walking."
        }
      ]
    },
    {
      "group": "Schemas list is empty",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "nonNullJsonNode" },
            { "parameter": "rootNode", "value": "nonNullJsonNode" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.of(\"/field\")" },
            { "parameter": "shouldValidateSchema", "value": false }
          ],
          "expected": "No schema.walk calls (list empty); method returns empty Set",
          "description": "When there are no inner schemas, the loop does nothing and the method should still return an empty Set."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "null" },
            { "parameter": "rootNode", "value": "null" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.of(\"/field\")" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "validate is NOT invoked; no schema.walk calls; returns empty Set",
          "description": "Even with an empty schemas list, a null node prevents validation, resulting in an empty Set."
        }
      ]
    }
  ]
}