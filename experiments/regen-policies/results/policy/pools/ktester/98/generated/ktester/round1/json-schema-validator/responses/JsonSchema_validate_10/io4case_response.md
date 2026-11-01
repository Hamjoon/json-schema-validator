{
  "tests": [
    {
      "group": "validate_validJson_defaultCustomizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "A well‑formed JSON document that satisfies the schema (required properties name:string and age:number). No customizer is supplied, so the method should return an empty validation result."
        }
      ]
    },
    {
      "group": "validate_invalidJson_syntaxError",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\", \"age\":30"   }
            ,
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "throws com.fasterxml.jackson.core.JsonParseException",
          "description": "The JSON string is syntactically malformed (missing closing brace). The deserialize step should raise a JsonParseException which propagates out of the validate method."
        }
      ]
    },
    {
      "group": "validate_jsonViolatesSchema",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "non‑empty Set<ValidationMessage> containing a message about missing required property 'age'",
          "description": "The JSON is syntactically correct but does not contain the required property 'age' defined by the schema. Validation should succeed syntactically and return a set with at least one ValidationMessage."
        }
      ]
    },
    {
      "group": "validate_nullInput",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": null
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "throws java.lang.IllegalArgumentException",
          "description": "Passing a null string for the JSON document is illegal; the method should detect this early and throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "validate_customizerInvoked",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumerThatSetsFlag"
            }
          ],
          "expected": "empty Set<ValidationMessage> and the flag on ExecutionContext is true",
          "description": "A non‑null Consumer<ExecutionContext> is supplied. The consumer sets a custom flag on the ExecutionContext. After validation the flag should be true, proving that the customizer was executed."
        }
      ]
    },
    {
      "group": "validate_customizerThrowsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumerThatThrowsRuntimeException"
            }
          ],
          "expected": "throws RuntimeException",
          "description": "If the supplied Consumer throws an unchecked exception while customizing the ExecutionContext, the validate method should propagate that exception."
        }
      ]
    },
    {
      "group": "validate_unsupportedEncoding",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "%E0%A4%A"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.URL_ENCODED"
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "throws java.io.UnsupportedEncodingException",
          "description": "When the input is URL‑encoded but contains an invalid percent‑encoding sequence, the internal URLDecoder used by deserialize should raise UnsupportedEncodingException."
        }
      ]
    },
    {
      "group": "validate_largeJsonPerformanceBoundary",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "largeJsonStringPlaceholder"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "empty Set<ValidationMessage> within acceptable time (e.g., < 2 seconds)",
          "description": "A very large but valid JSON document (e.g., ~5 MB) is supplied to test the method’s ability to handle large inputs without stack overflow or excessive latency."
        }
      ]
    }
  ]
}