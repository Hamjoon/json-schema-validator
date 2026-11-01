Invalid control character at: line 14 column 45 (char 352)
{
  "testCases": [
    {
      "group": "Validate true with array node matching schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "validArrayJsonNode   // e.g., [1,2,3] matching schema type array of integers"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set of ValidationMessage is empty (no errors) because the array conforms to the schema",
          "description": "When the schema expects an array of integers and the provided JsonNode is a valid array, walk should return an empty set of validation messages via the collecting output format."
        }
      ]
    },
    {
      "group": "Validate true with custom OutputFormat that returns a transformed result",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "validJsonNode   // e.g., {\"name\":\"John\"} matching a simple object schema"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<String>() { public String format(Set<ValidationMessage> messages) { return messages.isEmpty() ? \"OK\" : \"FAIL\"; } }"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "\"OK\"",
          "description": "A custom OutputFormat that converts the validation result into a simple string should be invoked after validation; for a valid node it must return \"OK\"."
        }
      ]
    },
    {
      "group": "Validate true with OutputFormat that returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "validJsonNode   // any node that satisfies the schema"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<Object>() { public Object format(Set<ValidationMessage> messages) { return null; } }"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "null",
          "description": "If the supplied OutputFormat deliberately returns null, walk should propagate that null value to the caller."
        }
      ]
    },
    {
      "group": "Null OutputFormat – should throw NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "validJsonNode"
            },
            {
              "parameter": "outputFormat",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "The walk method requires a non‑null OutputFormat; passing null must result in an NPE."
        }
      ]
    },
    {
      "group": "Validate false with custom OutputFormat that records invocation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "anyJsonNode   // content is irrelevant because validation is disabled"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<List<String>>() { private final List<String> log = new ArrayList<>(); public List<String> format(Set<ValidationMessage> messages) { log.add(\"format-called\"); return log; } }"
            },
            {
              "parameter": "validate",
              "value": false
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "[\"format-called\"]",
          "description": "Even when validation is disabled, the OutputFormat must still be invoked (with an empty message set). The custom format records that it was called and returns the log."
        }
      ]
    }
  ]
}