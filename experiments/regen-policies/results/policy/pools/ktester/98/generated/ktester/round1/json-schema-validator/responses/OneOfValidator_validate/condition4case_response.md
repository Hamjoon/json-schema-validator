{
  "group": "OneOfValidator.validate",
  "cases": [
    {
      "input": [
        { "parameter": "executionContext", "value": "mockExecutionContext" },
        { "parameter": "node", "value": "JsonNode representing {\"type\":\"A\"}" },
        { "parameter": "rootNode", "value": "same as node (root of instance)" },
        { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
      ],
      "expected": "empty Set (validation succeeds because exactly one subschema matches)",
      "description": "The instance matches only the first subschema (e.g., schema requiring property \"type\" = \"A\"). The validator should return an empty set of ValidationMessage, indicating success."
    },
    {
      "input": [
        { "parameter": "executionContext", "value": "mockExecutionContext" },
        { "parameter": "node", "value": "JsonNode representing {\"type\":\"B\"}" },
        { "parameter": "rootNode", "value": "same as node" },
        { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
      ],
      "expected": "Set containing a ValidationMessage with code \"oneOf\" and message indicating no subschema matches",
      "description": "The instance does not satisfy any of the defined subschemas. The validator must report a oneOf validation error."
    },
    {
      "input": [
        { "parameter": "executionContext", "value": "mockExecutionContext" },
        { "parameter": "node", "value": "JsonNode representing {\"type\":\"A\",\"extra\":\"value\"}" },
        { "parameter": "rootNode", "value": "same as node" },
        { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
      ],
      "expected": "Set containing a ValidationMessage with code \"oneOf\" and message indicating multiple subschemas match",
      "description": "The instance satisfies more than one subschema (e.g., both a schema that requires \"type\":\"A\" and another that allows any object with an \"extra\" field). The validator must report a oneOf validation error because more than one schema matches."
    },
    {
      "input": [
        { "parameter": "executionContext", "value": "mockExecutionContext" },
        { "parameter": "node", "value": "null JsonNode (missing instance)" },
        { "parameter": "rootNode", "value": "null JsonNode" },
        { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
      ],
      "expected": "empty Set (validation succeeds because a missing node is considered valid for oneOf)",
      "description": "When the instance node is null, the OneOfValidator should treat it as not applicable and return no validation messages."
    },
    {
      "input": [
        { "parameter": "executionContext", "value": "mockExecutionContextShortCircuitTrue" },
        { "parameter": "node", "value": "JsonNode representing {\"type\":\"A\"}" },
        { "parameter": "rootNode", "value": "same as node" },
        { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
      ],
      "expected": "empty Set (validation succeeds and validator short‑circuits after first matching subschema)",
      "description": "The validator’s `canShortCircuit()` returns true. As soon as the first subschema matches, validation stops and returns success without evaluating the remaining subschemas."
    },
    {
      "input": [
        { "parameter": "executionContext", "value": "mockExecutionContextShortCircuitFalse" },
        { "parameter": "node", "value": "JsonNode representing {\"type\":\"A\"}" },
        { "parameter": "rootNode", "value": "same as node" },
        { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
      ],
      "expected": "empty Set (validation succeeds after checking all subschemas)",
      "description": "The validator’s `canShortCircuit()` returns false. Even though the first subschema matches, the validator continues to evaluate all subschemas before returning success."
    }
  ]
}