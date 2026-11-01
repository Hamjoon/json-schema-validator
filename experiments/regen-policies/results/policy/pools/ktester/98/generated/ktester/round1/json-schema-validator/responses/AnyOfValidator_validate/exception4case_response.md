{
  "test_cases": [
    {
      "group": "AnyOfValidator - null executionContext should throw NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": null
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"string\",\"value\":\"test\"})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"type\":\"string\",\"value\":\"test\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "NullPointerException",
          "description": "The validator expects a non‑null ExecutionContext. Passing null must result in a NullPointerException before any schema processing occurs."
        }
      ]
    },
    {
      "group": "AnyOfValidator - null instanceLocation should throw NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"number\",\"value\":5})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"type\":\"number\",\"value\":5})"
            },
            {
              "parameter": "instanceLocation",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "A null JsonNodePath cannot be used to locate the instance node. The method should throw NullPointerException rather than silently ignoring the path."
        }
      ]
    },
    {
      "group": "AnyOfValidator - null rootNode should throw NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"boolean\",\"value\":true})"
            },
            {
              "parameter": "rootNode",
              "value": null
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "NullPointerException",
          "description": "The validator uses the root document for $ref resolution and other checks. Supplying a null rootNode must cause a NullPointerException."
        }
      ]
    },
    {
      "group": "AnyOfValidator - malformed anyOf array element should throw IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"string\",\"value\":\"foo\"})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"anyOf\":[42]})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The anyOf keyword must contain schema objects (or boolean schemas). An integer literal is invalid and should cause the validator to throw IllegalArgumentException during schema parsing."
        }
      ]
    },
    {
      "group": "AnyOfValidator - anyOf contains $ref to non‑existent schema should throw JsonSchemaException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"string\",\"value\":\"bar\"})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"anyOf\":[{\"$ref\":\"#/components/schemas/DoesNotExist\"}]})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "JsonSchemaException",
          "description": "When a subschema references a schema that cannot be resolved, the validator must raise a JsonSchemaException (or a subclass) indicating the unresolved reference."
        }
      ]
    },
    {
      "group": "AnyOfValidator - preloadJsonSchema throws IOException should propagate as JsonSchemaException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextPreloadFails"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"integer\",\"value\":1})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"anyOf\":[{\"$ref\":\"http://invalid-url/schema.json\"}]})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "JsonSchemaException",
          "description": "If preloadJsonSchema attempts to load an external schema and encounters an IOException, the validator should wrap and re‑throw it as a JsonSchemaException."
        }
      ]
    },
    {
      "group": "AnyOfValidator - canShortCircuit throws RuntimeException (mis‑configuration) should propagate",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextShortCircuitError"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"string\",\"value\":\"abc\"})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"anyOf\":[{\"type\":\"string\"}]})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "RuntimeException",
          "description": "A subclass that overrides canShortCircuit() to throw a RuntimeException (simulating a bug) must let that exception propagate; the validator should not catch it silently."
        }
      ]
    }
  ]
}