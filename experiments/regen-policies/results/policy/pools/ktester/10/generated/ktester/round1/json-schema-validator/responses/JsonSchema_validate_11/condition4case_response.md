{
  "groups": [
    {
      "group": "Valid JSON input with JSON InputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "new SimpleOutputFormat()" }
          ],
          "expected": "String containing no validation errors (e.g., \"[]\")",
          "description": "A well‑formed JSON document is supplied together with the JSON InputFormat and a simple OutputFormat that returns the validation messages as a JSON string. The method should deserialize the input, run validation (which succeeds for this minimal schema) and return the formatted result."
        }
      ]
    },
    {
      "group": "Valid YAML input with YAML InputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "name: John" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "format", "value": "new SimpleOutputFormat()" }
          ],
          "expected": "String containing no validation errors (e.g., \"[]\")",
          "description": "A well‑formed YAML document is supplied together with the YAML InputFormat. The method must use YamlMapperFactory to deserialize, then validate and format the result."
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
            { "parameter": "format", "value": "new SimpleOutputFormat()" }
          ],
          "expected": "IllegalArgumentException",
          "description": "Passing a null input should trigger an IllegalArgumentException before any deserialization occurs."
        }
      ]
    },
    {
      "group": "Empty input string",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "new SimpleOutputFormat()" }
          ],
          "expected": "IllegalArgumentException",
          "description": "An empty string cannot be parsed into a JsonNode; the method should detect this early and throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Invalid JSON syntax",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":John}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "new SimpleOutputFormat()" }
          ],
          "expected": "JsonProcessingException wrapped in RuntimeException",
          "description": "Malformed JSON should cause the underlying Jackson parser to throw a JsonProcessingException, which the method propagates (or wraps) as a runtime exception."
        }
      ]
    },
    {
      "group": "Invalid YAML syntax",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "name: John: Doe" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "format", "value": "new SimpleOutputFormat()" }
          ],
          "expected": "JsonProcessingException wrapped in RuntimeException",
          "description": "Malformed YAML should result in a parsing exception similar to the JSON case."
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
            { "parameter": "format", "value": "new SimpleOutputFormat()" }
          ],
          "expected": "IllegalArgumentException",
          "description": "If the InputFormat argument is null the method must reject the call before attempting deserialization."
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
            { "parameter": "format", "value": null }
          ],
          "expected": "IllegalArgumentException",
          "description": "A null OutputFormat cannot be used to format validation results; the method should throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Unsupported character encoding (simulated)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "%E0%A4%A" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "new SimpleOutputFormat()" }
          ],
          "expected": "UnsupportedEncodingException wrapped in RuntimeException",
          "description": "If URLDecoder.decode throws UnsupportedEncodingException (e.g., when the input contains malformed percent‑encoding), the method should propagate the exception."
        }
      ]
    },
    {
      "group": "Schema with required validator that fails",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "new SimpleOutputFormat()" }
          ],
          "expected": "String containing a validation error about missing required property \"name\"",
          "description": "The schema defines a required property \"name\". Supplying an empty JSON object should cause the validator to produce a ValidationMessage which the OutputFormat returns."
        }
      ]
    }
  ]
}