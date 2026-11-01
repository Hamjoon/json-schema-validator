{
  "testCases": [
    {
      "group": "Validation failure with missing required property",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"age\": 25 }"
            },
            {
              "parameter": "format",
              "value": "SimpleStringOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> {}"
            }
          ],
          "expected": "Formatted result containing a required‑field error (e.g., \"required property 'name' is missing\").",
          "description": "The schema defines a required property \"name\". The instance JSON lacks it, so validation should fail and the OutputFormat should render the corresponding ValidationMessage."
        }
      ]
    },
    {
      "group": "Validation failure with type mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": 123 }"
            },
            {
              "parameter": "format",
              "value": "SimpleStringOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> {}"
            }
          ],
          "expected": "Formatted result containing a type‑mismatch error (e.g., \"type mismatch: expected string but found integer\").",
          "description": "The schema expects \"name\" to be a string, but the instance provides a number. The method should report the type error via the OutputFormat."
        }
      ]
    },
    {
      "group": "Custom OutputFormat returning a complex object",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"valid\": true }"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> {}"
            }
          ],
          "expected": "An instance of java.util.List<ValidationMessage> containing zero elements (i.e., an empty list).",
          "description": "CollectingOutputFormat gathers all ValidationMessage objects into a List. For a valid instance the list should be empty, proving that generic return types are handled correctly."
        }
      ]
    },
    {
      "group": "Custom OutputFormat returning null",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"valid\": true }"
            },
            {
              "parameter": "format",
              "value": "NullReturningOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> {}"
            }
          ],
          "expected": "null",
          "description": "The OutputFormat implementation deliberately returns null. The validate method should propagate this null result without throwing, confirming that a null return value from the format is allowed."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer sets attribute read by OutputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"valid\": true }"
            },
            {
              "parameter": "format",
              "value": "AttributeAwareOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> executionContext.setAttribute(\"mode\", \"verbose\")"
            }
          ],
          "expected": "A formatted string that includes the word \"verbose\" (e.g., \"validation mode: verbose – no errors\").",
          "description": "The customizer stores an attribute in the ExecutionContext. AttributeAwareOutputFormat reads this attribute to decide how to render the result. This tests the data‑flow from the customizer to the format."
        }
      ]
    },
    {
      "group": "RootNode is an array and schema expects an array",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "[ { \"id\": 1 }, { \"id\": 2 } ]"
            },
            {
              "parameter": "format",
              "value": "SimpleStringOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> {}"
            }
          ],
          "expected": "A non‑null formatted result (empty string or success message) because the instance conforms to an array‑type schema.",
          "description": "The schema defines the root type as \"array\" with items of type \"object\" containing an integer \"id\". The provided JSON array matches, so validation should succeed."
        }
      ]
    },
    {
      "group": "RootNode contains additional property when additionalProperties is false",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"allowed\": \"yes\", \"extra\": \"no\" }"
            },
            {
              "parameter": "format",
              "value": "SimpleStringOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> {}"
            }
          ],
          "expected": "Formatted result containing an additional‑properties error (e.g., \"additional property 'extra' is not allowed\").",
          "description": "The schema sets \"additionalProperties\": false. The instance provides an unexpected property \"extra\", which must be reported by the validator and reflected in the formatted output."
        }
      ]
    }
  ]
}