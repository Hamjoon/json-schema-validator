{
  "newTestCases": [
    {
      "group": "Null executionContext – illegal argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "ObjectNode {\"foo\":\"bar\"}"
            },
            {
              "parameter": "outputFormat",
              "value": "mockOutputFormatReturningString"
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
          "description": "The method must reject a null ExecutionContext because it cannot store traversal state; a NullPointerException should be thrown before any other processing."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer throws – propagates exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode {\"valid\":true}"
            },
            {
              "parameter": "outputFormat",
              "value": "mockOutputFormatReturningString"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "mockConsumerThatThrowsRuntimeException"
            }
          ],
          "expected": "RuntimeException",
          "description": "If the supplied Consumer throws a RuntimeException, the walk method must not swallow it; the exception should propagate to the caller."
        }
      ]
    },
    {
      "group": "OutputFormat returns null – result is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode {\"a\":1}"
            },
            {
              "parameter": "outputFormat",
              "value": "mockOutputFormatReturningNull"
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
          "expected": "null",
          "description": "When the OutputFormat's `apply` method returns null, walk must forward that null value to the caller (no NPE should be thrown)."
        }
      ]
    },
    {
      "group": "Validate true – node fails schema validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode {\"age\":\"twenty\"}"  ,
              "description": "age should be an integer according to the schema"
            },
            {
              "parameter": "outputFormat",
              "value": "mockOutputFormatCollectingMessages"
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
          "expected": "List containing validation error for property 'age'",
          "description": "With `validate` true the method runs all validators; the mock OutputFormat aggregates the resulting ValidationMessage objects. Because the node violates the schema, the expected result is a collection that includes the specific error about the wrong type of `age`."
        }
      ]
    },
    {
      "group": "Validate true – nested object passes validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode {\"person\":{\"name\":\"Alice\",\"age\":30}}"
            },
            {
              "parameter": "outputFormat",
              "value": "mockOutputFormatReturningString"
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
          "expected": "\"nested validation passed\"",
          "description": "The schema contains a nested object definition. When the whole instance conforms, walk must traverse into the child node, invoke its validators, and finally the OutputFormat should receive a successful result (simulated by returning the string)."
        }
      ]
    },
    {
      "group": "Validate true – schema without validators (type‑agnostic)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode {}"
            },
            {
              "parameter": "outputFormat",
              "value": "mockOutputFormatReturningString"
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
          "expected": "\"no validators – treated as valid\"",
          "description": "If the schema does not declare any validators (e.g., an empty schema object), walk should still call the OutputFormat with an empty ValidationResult indicating success."
        }
      ]
    }
  ]
}