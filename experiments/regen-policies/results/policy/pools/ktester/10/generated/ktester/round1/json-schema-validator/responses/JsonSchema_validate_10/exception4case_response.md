{
  "testCases": [
    {
      "group": "Unsupported InputFormat – throws IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.XML"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException – the provided InputFormat is not supported by the method and should cause an immediate IllegalArgumentException before any deserialization occurs.",
          "description": "Passes an InputFormat value that the implementation does not recognize (e.g., XML). The method is expected to validate the format argument early and throw IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Malformed percent‑encoding in input – throws IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "%E0%ZZ"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException – URLDecoder.decode is invoked on the input string and the malformed percent‑encoding causes an IllegalArgumentException to be thrown.",
          "description": "Provides a string containing invalid URL‑encoded characters. The deserialization step attempts to URL‑decode the input, which should raise IllegalArgumentException due to the malformed sequence."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer throws RuntimeException – propagates exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "customizerThrowsRuntime"
            }
          ],
          "expected": "RuntimeException – the supplied ExecutionContextCustomizer throws a RuntimeException during customization, which should propagate out of the validate method.",
          "description": "Supplies a custom ExecutionContextCustomizer implementation that deliberately throws a RuntimeException. The test verifies that the validate method does not swallow the exception and lets it propagate to the caller."
        }
      ]
    }
  ]
}