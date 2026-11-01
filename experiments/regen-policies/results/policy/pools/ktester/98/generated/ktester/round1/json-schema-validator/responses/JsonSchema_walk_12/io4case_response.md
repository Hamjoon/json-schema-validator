{
  "testCases": [
    {
      "group": "walk_validStringNode_validatesSuccessfully",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultContext"
            },
            {
              "parameter": "node",
              "value": "\"hello\""
            },
            {
              "parameter": "outputFormat",
              "value": "SetOutputFormat"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "empty set",
          "description": "A JSON string node matches a schema that expects type \"string\". Validation is enabled, no customizer is supplied, so the walk should return an empty set of ValidationMessage objects."
        }
      ]
    },
    {
      "group": "walk_invalidStringNode_validationFails",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultContext"
            },
            {
              "parameter": "node",
              "value": "123"
            },
            {
              "parameter": "outputFormat",
              "value": "SetOutputFormat"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "set containing message \"$.type: expected type: String, found: Integer\"",
          "description": "A JSON numeric node is validated against a schema that requires a string. Validation is enabled, so the walk should return a set containing a single type‑mismatch ValidationMessage."
        }
      ]
    },
    {
      "group": "walk_validateFlagFalse_skipsValidation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultContext"
            },
            {
              "parameter": "node",
              "value": "123"
            },
            {
              "parameter": "outputFormat",
              "value": "SetOutputFormat"
            },
            {
              "parameter": "validate",
              "value": false
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "empty set",
          "description": "Even though the node does not satisfy the schema (type mismatch), the validate flag is false, so the walk should skip validation and return an empty set of messages."
        }
      ]
    },
    {
      "group": "walk_withExecutionCustomizerAddsCustomMessage",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultContext"
            },
            {
              "parameter": "node",
              "value": "\"test\""
            },
            {
              "parameter": "outputFormat",
              "value": "SetOutputFormat"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "addCustomMessageCustomizer"
            }
          ],
          "expected": "set containing message \"custom: added by ExecutionContextCustomizer\"",
          "description": "A custom ExecutionContextCustomizer injects a synthetic ValidationMessage into the ExecutionContext before validation runs. The walk should return a set that includes this custom message (and no schema errors for the valid string)."
        }
      ]
    },
    {
      "group": "walk_nullNode_throwsNullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultContext"
            },
            {
              "parameter": "node",
              "value": null
            },
            {
              "parameter": "outputFormat",
              "value": "SetOutputFormat"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null JsonNode to walk should result in a NullPointerException because the implementation expects a non‑null node to traverse."
        }
      ]
    }
  ]
}