{
  "testCases": [
    {
      "group": "RootNode is MissingNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "missingNode"
            },
            {
              "parameter": "format",
              "value": "mockFormatSuccess"
            }
          ],
          "expected": "outputSuccess",
          "description": "When the supplied JsonNode is a Jackson MissingNode (represents a non‑existent JSON value) the validator should treat it as a valid instance for an empty schema and forward an empty ValidationMessage set to the OutputFormat, which returns the predefined success value."
        }
      ]
    },
    {
      "group": "RootNode is JSON null with required type mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "jsonNullNode"
            },
            {
              "parameter": "format",
              "value": "mockFormatCollect"
            }
          ],
          "expected": "outputFailure",
          "description": "The schema requires a non‑null type (e.g., \"type\": \"object\"). Providing a JsonNode that represents JSON null should generate a ValidationMessage about type mismatch, which the mock OutputFormat collects and returns the predefined failure value."
        }
      ]
    },
    {
      "group": "OutputFormat returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "mockFormatReturnsNull"
            }
          ],
          "expected": "null",
          "description": "If the OutputFormat implementation returns null (instead of a concrete result) the validate method must propagate that null value to the caller."
        }
      ]
    },
    {
      "group": "OutputFormat returns custom object",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "mockFormatReturnsCustomObject"
            }
          ],
          "expected": "customResultObject",
          "description": "A custom OutputFormat that builds a domain‑specific DTO from the validation result should have its object returned unchanged by the validate method."
        }
      ]
    },
    {
      "group": "Schema with $ref resolution produces messages",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "invalidJsonNodeReferencingOtherSchema"
            },
            {
              "parameter": "format",
              "value": "mockFormatCollect"
            }
          ],
          "expected": "outputFailure",
          "description": "The schema contains a $ref to another schema file. The referenced schema defines a required property that is missing in the provided JSON, so validation yields a message. The mock OutputFormat collects the message and returns the predefined failure value."
        }
      ]
    },
    {
      "group": "Recursive anchor does not cause infinite loop",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNodeWithRecursiveAnchor"
            },
            {
              "parameter": "format",
              "value": "mockFormatSuccess"
            }
          ],
          "expected": "outputSuccess",
          "description": "A schema that uses the \"$recursiveAnchor\": true keyword can reference itself. Validation of a JSON document that satisfies the recursive definition must complete without stack overflow, returning an empty message set that the mock OutputFormat translates to success."
        }
      ]
    },
    {
      "group": "OutputFormat throws non‑Runtime Error",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "mockFormatThrowsError"
            }
          ],
          "expected": "Error",
          "description": "If the OutputFormat implementation throws a java.lang.Error (e.g., AssertionError) the validate method must not catch it; the Error should propagate unchanged to the caller."
        }
      ]
    }
  ]
}