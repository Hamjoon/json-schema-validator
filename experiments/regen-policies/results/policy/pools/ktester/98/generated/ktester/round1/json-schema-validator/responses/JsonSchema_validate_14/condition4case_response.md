Invalid control character at: line 18 column 51 (char 446)
{
  "testCases": [
    {
      "group": "Normal validation – happy path",
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
              "parameter": "format",
              "value": "new ListOutputFormat<>()   // returns List<ValidationMessage>"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\", true)"
            }
          ],
          "expected": "Empty list of ValidationMessage (no validation errors) and executionCustomizer must have been invoked (custom attribute set to true)",
          "description": "Validate a well‑formed JSON document using the JSON input format, a simple List‑based OutputFormat and a customizer that marks the ExecutionContext. The method should deserialize the input, run validation and return an empty list, while the customizer is called exactly once."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new ListOutputFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException when the lambda tries to invoke executionCustomizer.accept(...)",
          "description": "Passing a null consumer should cause a NPE because the internal lambda unconditionally calls executionCustomizer.accept(...)."
        }
      ]
    },
    {
      "group": "OutputFormat is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"valid\":true}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) thrown by the overloaded validate method because format is null",
          "description": "The method forwards the null OutputFormat to the overloaded validate; the contract of that overload requires a non‑null format, so an exception is expected."
        }
      ]
    },
    {
      "group": "Invalid JSON input – deserialize failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{invalidJson:}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new ListOutputFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "JsonProcessingException (or IOException) propagated from deserialize",
          "description": "When the input string cannot be parsed as JSON, deserialize throws an exception which is not caught inside the method, so the exception should bubble up."
        }
      ]
    },
    {
      "group": "Null input string",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "null"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new ListOutputFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) from deserialize because input is null",
          "description": "Passing a null input string should cause the deserialization step to fail, resulting in an exception."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer is invoked exactly once",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"id\":1}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new ListOutputFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "new CountingConsumer()   // increments a counter each call"
            }
          ],
          "expected": "Counter value equals 1 after validation completes",
          "description": "A custom Consumer implementation records how many times it is called. The lambda inside the method should invoke it exactly once, regardless of validation result."
        }
      ]
    }
  ]
}