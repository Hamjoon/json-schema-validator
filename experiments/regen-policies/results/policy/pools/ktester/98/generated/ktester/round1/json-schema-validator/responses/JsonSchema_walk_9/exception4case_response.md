{
  "testCases": [
    {
      "group": "Null InputFormat – argument validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": null
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
          "expected": "NullPointerException (or IllegalArgumentException) thrown because inputFormat is null",
          "description": "The walk method must reject a null InputFormat before attempting deserialization."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer throws AssertionError – error propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new AssertionError(\"customizer assertion failure\"); }"
            }
          ],
          "expected": "AssertionError with message 'customizer assertion failure' propagates out of walk",
          "description": "Even though the exception is an Error (not a RuntimeException), walk must not catch it and should let it bubble up."
        }
      ]
    },
    {
      "group": "Deep recursive schema without $recursiveAnchor – stack overflow protection",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"properties\":{\"a\":{\"$ref\":\"#/properties/b\"},\"b\":{\"$ref\":\"#/properties/a\"}}}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
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
          "expected": "ValidationResult.success == false and ValidationMessage mentions recursion depth limit or StackOverflowError is avoided",
          "description": "A schema that references each other without a $recursiveAnchor should be detected as too deep; the implementation must guard against infinite recursion and return a failure result rather than crashing."
        }
      ]
    },
    {
      "group": "Invalid percent‑encoding in input – UnsupportedEncodingException handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "%ZZ%GG"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.URL_ENCODED"
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
          "expected": "ValidationResult.success == false and ValidationMessage contains a decoding error (UnsupportedEncodingException)",
          "description": "When the input format expects URL‑encoded data, malformed percent‑encoding should trigger an UnsupportedEncodingException which must be reported as a parsing failure."
        }
      ]
    },
    {
      "group": "Unsupported enum constant for InputFormat – illegal argument handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.CSV"
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
          "expected": "IllegalArgumentException thrown indicating that the supplied InputFormat is not supported",
          "description": "If a caller passes an InputFormat value that the deserialize method does not handle (e.g., CSV), walk must reject it with an appropriate exception."
        }
      ]
    }
  ]
}