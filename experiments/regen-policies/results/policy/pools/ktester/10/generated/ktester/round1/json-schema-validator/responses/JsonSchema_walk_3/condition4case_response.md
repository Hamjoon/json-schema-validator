Invalid control character at: line 50 column 84 (char 2214)
{
  "testCases": [
    {
      "group": "Valid inputs – JSON format, validation enabled",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\",\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult with isSuccess() == true and no validation messages",
          "description": "A well‑formed JSON document is supplied. The method should deserialize the input, create an execution context and walk the schema while performing validation, resulting in a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Valid inputs – YAML format, validation enabled",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "name: John\nage: 30" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult with isSuccess() == true and no validation messages",
          "description": "A well‑formed YAML document is supplied. The method should correctly use the YAML mapper, deserialize to a JsonNode and validate successfully."
        }
      ]
    },
    {
      "group": "Validation flag disabled – JSON format",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\",\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": false }
          ],
          "expected": "ValidationResult with isSuccess() == true and no validation messages (validation step skipped)",
          "description": "When the `validate` flag is false the walk should still traverse the schema but must not perform any validation checks, therefore the result should be successful regardless of schema constraints."
        }
      ]
    },
    {
      "group": "Invalid JSON – parsing error",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\",\"age\":30" },   " // missing closing brace"
            ,
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "throws JsonProcessingException (or ValidationResult with errors indicating parsing failure)",
          "description": "The JSON string is malformed. `deserialize` should raise a parsing exception which propagates out of `walk`. The test verifies that the exception type is as expected."
        }
      ]
    },
    {
      "group": "Invalid YAML – parsing error",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "name: John\nage: [30" },   " // malformed YAML list"
            ,
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "throws JsonProcessingException (or ValidationResult with errors indicating parsing failure)",
          "description": "The YAML document cannot be parsed. The method should surface the parsing exception."
        }
      ]
    },
    {
      "group": "Null input string",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": null },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "throws NullPointerException",
          "description": "Passing a null `input` should cause a NullPointerException when `deserialize` attempts to process it."
        }
      ]
    },
    {
      "group": "Null InputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": null },
            { "parameter": "validate", "value": true }
          ],
          "expected": "throws NullPointerException",
          "description": "A null `inputFormat` cannot be used for deserialization and should result in a NullPointerException."
        }
      ]
    },
    {
      "group": "Unsupported character encoding (simulated)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "%E0%A4%A" },   " // malformed percent‑encoding"
            ,
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "throws UnsupportedEncodingException",
          "description": "If the input contains malformed URL‑encoded characters, `deserialize` may trigger an UnsupportedEncodingException. The test ensures this exception propagates."
        }
      ]
    }
  ]
}