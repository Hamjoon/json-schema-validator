{
  "testCases": [
    {
      "group": "shouldValidateSchema true",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "mockNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "the Set returned by AnyOfValidator.validate(...) is returned unchanged",
          "description": "When shouldValidateSchema is true the method must delegate to validate(...) and return its result. No schema walk is performed."
        }
      ]
    },
    {
      "group": "shouldValidateSchema false – empty schemas list",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "mockNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": false
            }
          ],
          "expected": "empty LinkedHashSet",
          "description": "When shouldValidateSchema is false and the internal schemas list is empty, the method should iterate over zero schemas, invoke no walk calls, and return an empty set."
        }
      ]
    },
    {
      "group": "shouldValidateSchema false – multiple schemas",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "mockNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": false
            }
          ],
          "expected": "empty LinkedHashSet and each schema.walk(...) called once with shouldValidateSchema=false",
          "description": "When shouldValidateSchema is false and the validator holds several JsonSchema objects, walk must invoke each schema's walk method with the same parameters and shouldValidateSchema set to false, then return an empty set."
        }
      ]
    },
    {
      "group": "shouldValidateSchema true – validate returns non‑empty set",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "mockNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "Set containing ValidationMessage objects as returned by validate(...)",
          "description": "Ensures that the exact Set produced by validate(...) is propagated back to the caller when shouldValidateSchema is true."
        }
      ]
    }
  ]
}