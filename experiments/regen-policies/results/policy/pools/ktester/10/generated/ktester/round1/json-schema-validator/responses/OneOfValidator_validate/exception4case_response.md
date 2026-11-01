{
  "test_cases": [
    {
      "group": "Fail‑fast mode – multiple matching schemas",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(walkEnabled=false, failFast=true, validationEnabled=true, schemas=[validSchema, validSchema])"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(validNode)"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode(root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath(\"/path\")"
            }
          ],
          "expected": "throws JsonSchemaException containing OneOf ValidationMessage (numberOfValidSchema == 2)",
          "description": "failFast is true and two schemas both validate without errors, so numberOfValidSchema becomes 2. The validator short‑circuits the loop and then throws a JsonSchemaException instead of returning an error set."
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
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(anyNode)"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode(root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath(\"/path\")"
            }
          ],
          "expected": "throws NullPointerException",
          "description": "The method dereferences executionContext immediately to obtain CollectorContext; passing null triggers a NullPointerException."
        }
      ]
    },
    {
      "group": "Null collectorContext inside executionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(collectorContext=null, walkEnabled=false, failFast=false, validationEnabled=true)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(anyNode)"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode(root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath(\"/path\")"
            }
          ],
          "expected": "throws NullPointerException",
          "description": "The validator calls executionContext.getCollectorContext().enterDynamicScope(); if the collectorContext is null a NullPointerException is thrown."
        }
      ]
    },
    {
      "group": "Null schema element in schemas list",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(walkEnabled=false, failFast=false, validationEnabled=true, schemas=[validSchema, null])"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(validNode)"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode(root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath(\"/path\")"
            }
          ],
          "expected": "throws NullPointerException",
          "description": "During iteration the code invokes schema.validate on a null entry, causing a NullPointerException."
        }
      ]
    },
    {
      "group": "Schema.validate returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(walkEnabled=false, failFast=false, validationEnabled=true, schemas=[nullReturningValidateSchema])"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(anyNode)"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode(root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath(\"/path\")"
            }
          ],
          "expected": "throws NullPointerException",
          "description": "If a schema's validate method returns null, the subsequent call to schemaErrors.isEmpty() triggers a NullPointerException."
        }
      ]
    },
    {
      "group": "Schema.walk returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(walkEnabled=true, failFast=false, validationEnabled=true, schemas=[nullReturningWalkSchema])"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(anyNode)"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode(root)"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath(\"/path\")"
            }
          ],
          "expected": "throws NullPointerException",
          "description": "When walk is enabled the validator calls schema.walk; if that method returns null, the later schemaErrors.isEmpty() call throws a NullPointerException."
        }
      ]
    }
  ]
}