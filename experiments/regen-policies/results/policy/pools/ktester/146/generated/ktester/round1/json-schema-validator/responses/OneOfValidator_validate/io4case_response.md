{
  "testCases": [
    {
      "group": "Valid – oneOf with a single subschema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (default settings)"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"age\":30}"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "empty Set (no ValidationMessage), because the sole subschema validates successfully",
          "description": "When a oneOf array contains exactly one schema, the validator should behave like that schema. The instance satisfies the subschema, so validation succeeds."
        }
      ]
    },
    {
      "group": "Invalid – multiple subschemas match and canShortCircuit is false",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (canShortCircuit() returns false)"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"type\":\"C\", \"shared\":\"yes\"}"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "Set containing a ValidationMessage indicating that more than one schema matched (oneOf violation)",
          "description": "With short‑circuiting disabled the validator must evaluate all subschemas. The instance satisfies two of them, so a oneOf error should be reported."
        }
      ]
    },
    {
      "group": "Valid – multiple subschemas match but canShortCircuit is true (short‑circuit semantics)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (canShortCircuit() returns true)"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"type\":\"D\", \"extra\":\"value\"}"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "empty Set (no ValidationMessage), because validation stops after the first successful subschema",
          "description": "When short‑circuiting is enabled the validator stops after the first matching subschema, even if later subschemas would also match. The expected behaviour is a successful validation."
        }
      ]
    },
    {
      "group": "Report child errors – true (default) aggregates messages from failing subschemas",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (reportChildErrors() returns true)"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"type\":\"E\", \"age\":-5}"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "Set containing at least two ValidationMessages: one for the oneOf rule and one (or more) from the child schema(s) reporting the 'minimum' violation on 'age'",
          "description": "When child errors are reported, the validator should return the top‑level oneOf message plus any messages produced by the subschemas that were evaluated and failed."
        }
      ]
    },
    {
      "group": "Valid – array instance matches exactly one subschema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing [1,2,3]"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "empty Set (no ValidationMessage)",
          "description": "The oneOf contains two array‑type subschemas: one requiring exactly three items, another requiring a minimum of five items. The instance matches only the first, so validation succeeds."
        }
      ]
    },
    {
      "group": "Invalid – $ref subschema resolves and causes multiple matches",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (schema contains $ref to '#/definitions/common')"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"common\":\"value\", \"extra\":\"x\"}"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "Set containing a ValidationMessage indicating that more than one schema matched (oneOf violation)",
          "description": "One subschema is a $ref to a definition that validates the object, and another subschema independently validates the same structure. Both match, so a oneOf error must be reported."
        }
      ]
    },
    {
      "group": "Valid – oneOf with non‑object schema (type string)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing \"hello\""
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "empty Set (no ValidationMessage), because the string matches exactly one of the subschemas (type: string, maxLength: 10)",
          "description": "The oneOf array contains a string schema with maxLength 10 and a number schema. The instance is a valid string within the length limit, so only one subschema matches."
        }
      ]
    },
    {
      "group": "Invalid – duplicate identical subschemas cause multiple matches",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"flag\":true}"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "Set containing a ValidationMessage stating that more than one schema matched (oneOf violation)",
          "description": "The oneOf list contains two identical schemas that both accept the instance. Even though they are duplicates, the validator must detect that more than one schema matches and report an error."
        }
      ]
    }
  ]
}