{
  "testCases": [
    {
      "group": "Null schemas list",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "nonNullJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "nonNullJsonNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.of(\"/field\")"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": false
            }
          ],
          "expected": "NullPointerException",
          "description": "When the internal `schemas` field is null, the for‑each loop attempts to iterate over a null collection and throws a NullPointerException."
        }
      ]
    },
    {
      "group": "Schemas list contains null element",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "nonNullJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "nonNullJsonNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.of(\"/field\")"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": false
            }
          ],
          "expected": "NullPointerException",
          "description": "If the `schemas` list contains a null entry, the iteration reaches that entry and attempts to invoke `schema.walk(...)` on a null reference, resulting in a NullPointerException."
        }
      ]
    },
    {
      "group": "Null executionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": null
            },
            {
              "parameter": "node",
              "value": "nonNullJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "nonNullJsonNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.of(\"/field\")"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "NullPointerException",
          "description": "When `shouldValidateSchema` is true and `node` is non‑null, `walk` delegates to `validate`. Passing a null `executionContext` causes `validate` (or downstream code) to throw a NullPointerException."
        }
      ]
    },
    {
      "group": "Null instanceLocation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "nonNullJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "nonNullJsonNode"
            },
            {
              "parameter": "instanceLocation",
              "value": null
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "NullPointerException",
          "description": "If `instanceLocation` is null while validation is requested, the call to `validate` receives a null argument and throws a NullPointerException."
        }
      ]
    },
    {
      "group": "Validate method throws unchecked exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "nonNullJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "nonNullJsonNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.of(\"/field\")"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "IllegalStateException",
          "description": "When `validate` is mocked to throw an unchecked exception (e.g., IllegalStateException), `walk` must not catch it; the exception propagates to the caller."
        }
      ]
    }
  ]
}