{
  "testCases": [
    {
      "group": "Valid JSON input with successful validation",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\",\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>> (collects messages)" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "An empty list (no validation messages) is returned, indicating the JSON conforms to the schema.",
          "description": "When a well‑formed JSON document that satisfies the schema is supplied, the validator should return an empty collection of ValidationMessage objects."
        }
      ]
    },
    {
      "group": "Invalid JSON input (syntax error)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\", \"age\":30" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>>" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "A list containing a single ValidationMessage describing a JSON parsing error (e.g., \"Unexpected end-of-input\") is returned.",
          "description": "If the input string cannot be parsed as JSON, the deserialize step throws an IOException which is translated into a validation message."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "<xml><name>John</name></xml>" },
            { "parameter": "inputFormat", "value": "InputFormat.XML" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>>" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "An IllegalArgumentException (or a ValidationMessage indicating unsupported format) is thrown.",
          "description": "The method should reject formats that it does not know how to deserialize."
        }
      ]
    },
    {
      "group": "Null consumer (no execution customization)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>>" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "An empty list is returned (validation succeeds) and no NullPointerException is thrown.",
          "description": "The method must handle a null Consumer gracefully, treating it as a no‑op customizer."
        }
      ]
    },
    {
      "group": "Consumer throws RuntimeException",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>>" },
            { "parameter": "executionCustomizer", "value": "consumer that throws IllegalStateException" }
          ],
          "expected": "The IllegalStateException propagates out of the validate method.",
          "description": "If the provided Consumer throws an exception, the validator should not swallow it; the exception must be re‑thrown."
        }
      ]
    },
    {
      "group": "Null input string",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "null" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>>" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "An IllegalArgumentException (or a ValidationMessage indicating that the input is null) is thrown.",
          "description": "Passing a null JSON string should be rejected early, before deserialization."
        }
      ]
    },
    {
      "group": "Null OutputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "null" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "A NullPointerException (or IllegalArgumentException) is thrown because the format argument is required.",
          "description": "The method must validate that the OutputFormat argument is not null before proceeding."
        }
      ]
    },
    {
      "group": "Deserialization throws UnsupportedEncodingException",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "%E0%A4%A" },   0,
            { "parameter": "inputFormat", "value": "InputFormat.URL_ENCODED" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>>" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "A ValidationMessage describing the encoding problem is returned inside the list.",
          "description": "When URLDecoder fails due to malformed percent‑encoding, the deserialize method catches UnsupportedEncodingException and converts it into a validation message."
        }
      ]
    }
  ]
}