{
  "testCases": [
    {
      "group": "AnyOfValidator - node null but anyOf allows null",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextAllowNull" },
            { "parameter": "node", "value": "null" },
            { "parameter": "rootNode", "value": "jsonNode({\"type\":\"object\",\"properties\":{\"maybeNull\":{\"anyOf\":[{\"type\":\"null\"},{\"type\":\"string\"}]}}})" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root().append(\"maybeNull\")" }
          ],
          "expected": "empty Set<ValidationMessage> (null instance matches the 'type\":\"null' subschema)",
          "description": "When the instance node is null and the anyOf array contains a subschema that explicitly permits null, validation must succeed and return no errors."
        }
      ]
    },
    {
      "group": "AnyOfValidator - anyOf contains boolean true schema (always passes)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "jsonNode({\"type\":\"integer\",\"value\":42})" },
            { "parameter": "rootNode", "value": "jsonNode({\"type\":\"integer\",\"value\":42})" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "empty Set<ValidationMessage> (anyOf includes a true schema which validates any instance)",
          "description": "A boolean schema `true` inside anyOf should cause validation to succeed regardless of the other subschemas."
        }
      ]
    },
    {
      "group": "AnyOfValidator - anyOf contains boolean false schema (fails unless another subschema passes)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "jsonNode({\"type\":\"string\",\"value\":\"foo\"})" },
            { "parameter": "rootNode", "value": "jsonNode({\"type\":\"string\",\"value\":\"foo\"})" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "empty Set<ValidationMessage> (second subschema matches, first is false)",
          "description": "The anyOf array contains a `false` schema (which never matches) followed by a valid string schema. Validation must succeed because at least one subschema matches."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "jsonNode({\"type\":\"number\",\"value\":3.14})" },
            { "parameter": "rootNode", "value": "jsonNode({\"type\":\"number\",\"value\":3.14})" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "Set<ValidationMessage> containing a single anyOf error (no subschema matches)",
          "description": "Both subschemas fail (first is false, second expects a string). The validator must return a single anyOf error, not two."
        }
      ]
    },
    {
      "group": "AnyOfValidator - anyOf contains empty schema {} (matches any node)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "jsonNode({\"type\":\"array\",\"value\":[1,2,3]})" },
            { "parameter": "rootNode", "value": "jsonNode({\"type\":\"array\",\"value\":[1,2,3]})" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "empty Set<ValidationMessage> (empty schema matches everything)",
          "description": "An empty schema `{}` inside anyOf should validate any instance successfully."
        }
      ]
    },
    {
      "group": "AnyOfValidator - duplicate subschemas, ensure single success",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextDuplicate" },
            { "parameter": "node", "value": "jsonNode({\"type\":\"string\",\"value\":\"dup\"})" },
            { "parameter": "rootNode", "value": "jsonNode({\"type\":\"string\",\"value\":\"dup\"})" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "empty Set<ValidationMessage> (duplicate schemas both match, but validator returns no errors)",
          "description": "anyOf may contain identical subschemas. Validation should succeed after the first match; duplicate entries must not cause extra errors."
        }
      ]
    },
    {
      "group": "AnyOfValidator - anyOf with const value matching",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextConst" },
            { "parameter": "node", "value": "jsonNode({\"type\":\"string\",\"value\":\"fixed\"})" },
            { "parameter": "rootNode", "value": "jsonNode({\"type\":\"string\",\"value\":\"fixed\"})" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "empty Set<ValidationMessage> (const subschema matches)",
          "description": "A subschema with `\"const\":\"fixed\"` should cause validation to succeed when the instance equals that constant."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextConst" },
            { "parameter": "node", "value": "jsonNode({\"type\":\"string\",\"value\":\"different\"})" },
            { "parameter": "rootNode", "value": "jsonNode({\"type\":\"string\",\"value\":\"different\"})" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "Set<ValidationMessage> with a single anyOf error (const does not match)",
          "description": "When the const value does not match, and no other subschema matches, anyOf must produce one error."
        }
      ]
    },
    {
      "group": "AnyOfValidator - anyOf with enum matching",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextEnum" },
            { "parameter": "node", "value": "jsonNode({\"type\":\"string\",\"value\":\"red\"})" },
            { "parameter": "rootNode", "value": "jsonNode({\"type\":\"string\",\"value\":\"red\"})" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "empty Set<ValidationMessage> (enum subschema matches)",
          "description": "A subschema with an `enum` that includes the instance value should cause validation to succeed."
        }
      ]
    },
    {
      "group": "AnyOfValidator - single error aggregation when all subschemas fail",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextAggregate" },
            { "parameter": "node", "value": "jsonNode({\"type\":\"integer\",\"value\":7})" },
            { "parameter": "rootNode", "value": "jsonNode({\"type\":\"integer\",\"value\":7})" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "Set<ValidationMessage> size == 1, type='anyOf'",
          "description": "Even though multiple subschemas (e.g., string, boolean, object) fail, AnyOfValidator must return exactly one ValidationMessage describing the anyOf failure."
        }
      ]
    },
    {
      "group": "AnyOfValidator - short‑circuit disabled, all subschemas evaluated",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextNoShortCircuitCounter" },
            { "parameter": "node", "value": "jsonNode({\"type\":\"string\",\"value\":\"abc\"})" },
            { "parameter": "rootNode", "value": "jsonNode({\"type\":\"string\",\"value\":\"abc\"})" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" }
          ],
          "expected": "empty Set<ValidationMessage> and counter == 3 (all three subschemas validated)",
          "description": "When canShortCircuit() returns false, the validator must evaluate every subschema even after a successful match. A mock subschema validator increments a shared counter; the test asserts the counter equals the number of subschemas."
        }
      ]
    },
    {
      "group": "AnyOfValidator - validation of a nested child node (non‑root location)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextNested" },
            { "parameter": "node", "value": "jsonNode({\"type\":\"object\",\"properties\":{\"inner\":{\"type\":\"number\",\"value\":9}}})" },
            { "parameter": "rootNode", "value": "jsonNode({\"type\":\"object\",\"properties\":{\"inner\":{\"type\":\"number\",\"value\":9}}})" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root().append(\"inner\")" }
          ],
          "expected": "empty Set<ValidationMessage> (anyOf validates the child node correctly)",
          "description": "The validator receives an instanceLocation pointing to a nested property. It must correctly evaluate anyOf against the child node while preserving the proper path in any generated messages."
        }
      ]
    }
  ]
}