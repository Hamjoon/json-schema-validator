{
  "testCases": [
    {
      "group": "anyOf validation succeeds (short‑circuit off)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (short‑circuit disabled)"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"string\", \"value\": \"hello\" }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"string\", \"value\": \"hello\" }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "empty Set<ValidationMessage> – the instance satisfies at least one subschema, validator does not short‑circuit, walk flag = false",
          "description": "The AnyOfValidator should iterate over all subschemas, find the first that validates successfully, but because short‑circuit is disabled it continues checking the rest; the final result is still an empty error set."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (short‑circuit disabled)"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"integer\", \"value\": 5 }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"integer\", \"value\": 5 }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "empty Set<ValidationMessage> – second subschema validates, validator still evaluates remaining subschemas because short‑circuit is off, final set stays empty",
          "description": "Ensures that when the first subschema fails but a later one passes, the validator still returns success even without short‑circuiting."
        }
      ]
    },
    {
      "group": "anyOf validation succeeds (short‑circuit on)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (short‑circuit enabled)"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"string\", \"value\": \"short\" }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"string\", \"value\": \"short\" }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "empty Set<ValidationMessage> – validator stops after the first matching subschema because canShortCircuit() returns true",
          "description": "Verifies that when short‑circuiting is allowed the validator exits early after a successful subschema, skipping the remaining checks."
        }
      ]
    },
    {
      "group": "anyOf validation fails (all subschemas reject)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (any configuration)"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"boolean\", \"value\": true }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"boolean\", \"value\": true }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "Set<ValidationMessage> containing at least one message per subschema – because none of the anyOf alternatives validate, the validator aggregates all errors",
          "description": "Checks that when the instance does not satisfy any subschema, the validator returns a non‑empty error set that includes messages from each failed subschema."
        }
      ]
    },
    {
      "group": "null or missing instance node",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "empty Set<ValidationMessage> – validator treats null as absent and, according to JSON‑Schema spec, anyOf is ignored for missing nodes (unless required)",
          "description": "Ensures that a null instance does not cause a NullPointerException and results in a successful validation when the node is absent."
        }
      ]
    },
    {
      "group": "walk flag true (deep validation)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"object\", \"inner\": { \"type\": \"string\", \"value\": \"deep\" } }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"inner\": { \"type\": \"string\", \"value\": \"deep\" } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "empty Set<ValidationMessage> – validator is invoked with walk=true, causing it to traverse child nodes; all child subschemas validate, so result is empty",
          "description": "Validates that the overloaded protected validate method correctly propagates the walk flag and performs deep validation without adding extra errors."
        }
      ]
    },
    {
      "group": "exception handling – malformed schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"string\", \"value\": \"test\" }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"string\", \"value\": \"test\" }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "throws SchemaException (or a RuntimeException) – the AnyOfValidator is constructed with a malformed anyOf array (e.g., null element), causing validation to fail with an exception",
          "description": "Confirms that the validator does not swallow internal errors; a malformed schema should surface as an exception."
        }
      ]
    }
  ]
}