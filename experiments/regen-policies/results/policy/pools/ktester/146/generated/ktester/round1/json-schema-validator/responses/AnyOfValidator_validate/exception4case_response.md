{
  "test_cases": [
    {
      "group": "exception handling – null executionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
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
          "expected": "NullPointerException",
          "description": "The validator should reject a null ExecutionContext and throw a NullPointerException before any schema processing occurs."
        }
      ]
    },
    {
      "group": "exception handling – null instanceLocation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"integer\", \"value\": 10 }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"integer\", \"value\": 10 }"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "When the JsonNodePath argument is null the validator cannot compute the instance location and must raise an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "exception handling – anyOf array contains null element",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (anyOf contains null element)"
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
          "expected": "NullPointerException",
          "description": "If the anyOf keyword is defined with an array that includes a null entry, iteration over the subschemas will hit a null reference and the validator should throw a NullPointerException."
        }
      ]
    },
    {
      "group": "exception handling – anyOf array contains non‑schema element",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (anyOf contains primitive element)"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"string\", \"value\": \"abc\" }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"string\", \"value\": \"abc\" }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "ClassCastException",
          "description": "When the anyOf array holds a JSON primitive (e.g., a number) instead of a schema object, the validator will attempt to cast it to a JsonSchema and should raise a ClassCastException."
        }
      ]
    },
    {
      "group": "exception handling – rootNode null while node non‑null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"array\", \"value\": [1,2,3] }"
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
          "expected": "NullPointerException",
          "description": "The validator relies on the rootNode for $ref resolution; passing a null rootNode while the instance node is present should cause a NullPointerException."
        }
      ]
    }
  ]
}