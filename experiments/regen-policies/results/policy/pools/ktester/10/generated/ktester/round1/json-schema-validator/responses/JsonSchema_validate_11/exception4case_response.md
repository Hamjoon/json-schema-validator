{
  "test_cases": [
    {
      "group": "Malformed percent‑encoding causing IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "%E0%A4"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The input string contains an incomplete percent‑escape sequence. URLDecoder.decode throws IllegalArgumentException before deserialization, which should be propagated by the validate method."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat value (simulated enum mismatch)",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.UNKNOWN"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "If an InputFormat value that the method does not recognise (e.g., a future enum constant) is supplied, the internal switch should reject it and throw IllegalArgumentException."
        }
      ]
    },
    {
      "group": "OutputFormat throws checked Exception wrapped in RuntimeException",
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
              "value": "new CheckedThrowingOutputFormat()"
            }
          ],
          "expected": "RuntimeException wrapping the checked exception from OutputFormat",
          "description": "A custom OutputFormat implementation throws a checked exception (e.g., IOException) from its format method. The validate method must wrap this checked exception into a RuntimeException and propagate it."
        }
      ]
    }
  ]
}