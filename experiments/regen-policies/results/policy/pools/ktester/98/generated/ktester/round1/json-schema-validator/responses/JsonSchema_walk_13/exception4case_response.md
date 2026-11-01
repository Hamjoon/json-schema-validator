{
  "test_cases": [
    {
      "group": "Null InputFormat and Null OutputFormat – InputFormat validation precedes OutputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"key\\\":\\\"value\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            },
            {
              "parameter": "outputFormat",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "IllegalArgumentException with message containing 'InputFormat cannot be null'",
          "description": "Both InputFormat and OutputFormat are null, but the method should fail fast on the missing InputFormat before checking the OutputFormat, raising an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Unsupported URL‑encoded InputFormat – deserializer throws UnsupportedEncodingException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"%ZZ%\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.URL_ENCODED"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<ValidationResult>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "RuntimeException wrapping UnsupportedEncodingException",
          "description": "When the input is malformed URL‑encoded data, the private `deserialize` method (which uses `URLDecoder`) throws an UnsupportedEncodingException. The `walk` method must propagate this as a RuntimeException."
        }
      ]
    },
    {
      "group": "OutputFormat throws a checked exception (IOException) – should be wrapped",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"a\\\":1}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new IOExceptionThrowingOutputFormat<ValidationResult>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "RuntimeException wrapping IOException",
          "description": "A custom `OutputFormat` implementation deliberately throws an IOException during `apply`. The `walk` method must not swallow the exception; it should be wrapped in a RuntimeException and propagated to the caller."
        }
      ]
    },
    {
      "group": "ExecutionContext null and InputFormat null – InputFormat validation still triggers IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"x\\\":10}\""
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<ValidationResult>()"
            },
            {
              "parameter": "validate",
              "value": "false"
            }
          ],
          "expected": "IllegalArgumentException with message containing 'InputFormat cannot be null'",
          "description": "Even when the supplied `ExecutionContext` is null (which the method would normally replace with a default), the first argument check for a non‑null `InputFormat` must fire, resulting in an IllegalArgumentException."
        }
      ]
    }
  ]
}