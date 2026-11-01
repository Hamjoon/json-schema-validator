{
  "testCases": [
    {
      "group": "Valid – exactly one subschema matches",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "JsonNode representing {\"type\":\"A\"}" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "empty Set (no ValidationMessage), because only one subschema validates",
          "description": "The JSON instance matches exactly one of the 'oneOf' subschemas, so validation should succeed with no errors."
        }
      ]
    },
    {
      "group": "Invalid – no subschema matches",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "JsonNode representing {\"type\":\"UNKNOWN\"}" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "Set containing a ValidationMessage with message containing 'oneOf' and 'must match exactly one schema'",
          "description": "The JSON instance does not satisfy any of the subschemas, therefore OneOfValidator must return a validation error."
        }
      ]
    },
    {
      "group": "Invalid – more than one subschema matches",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "JsonNode representing {\"type\":\"B\", \"common\":\"value\"}" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "Set containing a ValidationMessage indicating that more than one schema matched",
          "description": "The instance satisfies two subschemas, violating the 'oneOf' rule; validator must report an error."
        }
      ]
    },
    {
      "group": "Edge – node is null",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "null" },
            { "parameter": "rootNode", "value": "null" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "empty Set (validation passes) if schema allows null, otherwise a ValidationMessage about type mismatch",
          "description": "When the instance node is null, OneOfValidator should either succeed (if null is permitted by any subschema) or fail; this case expects the subschemas to allow null, resulting in no errors."
        }
      ]
    },
    {
      "group": "Edge – schema contains no subschemas (empty oneOf)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "JsonNode representing {}" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "Set containing a ValidationMessage stating that 'oneOf' must contain at least one schema",
          "description": "If the OneOfValidator is constructed with an empty list of subschemas, validation should fail immediately with a clear error."
        }
      ]
    },
    {
      "group": "Short‑circuit enabled – first matching subschema stops further checks",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext (canShortCircuit() returns true)" },
            { "parameter": "node", "value": "JsonNode representing {\"type\":\"A\"}" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "empty Set, and verification that only the first matching subschema’s validate method was invoked",
          "description": "When canShortCircuit() is true, OneOfValidator should stop evaluating remaining subschemas after the first successful validation, resulting in no errors."
        }
      ]
    },
    {
      "group": "Report child errors – false (errors from child validators are suppressed)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext (reportChildErrors() returns false)" },
            { "parameter": "node", "value": "JsonNode representing {\"type\":\"B\"}" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "Set containing a single ValidationMessage for the oneOf rule, without propagating child schema messages",
          "description": "If reportChildErrors() is false, validation should return only the top‑level oneOf error even when child validators produce multiple messages."
        }
      ]
    },
    {
      "group": "Walk flag true – validator invoked via walk method",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "JsonNode representing {\"type\":\"A\"}" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "empty Set, same as validate() result, confirming walk delegates correctly",
          "description": "Calling walk with shouldValidateSchema=true should produce the same result as validate(), ensuring the delegation logic works."
        }
      ]
    }
  ]
}