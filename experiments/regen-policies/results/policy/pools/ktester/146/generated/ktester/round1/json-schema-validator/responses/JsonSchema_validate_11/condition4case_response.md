{
  "group": "JsonSchema.validate(String, InputFormat, ExecutionContextCustomizer) - branch coverage",
  "cases": [
    {
      "input": [
        { "parameter": "input", "value": "{\"name\":\"John\",\"age\":30}" },
        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
        { "parameter": "executionCustomizer", "value": "null" }
      ],
      "expected": "Set<ValidationMessage> is empty (no validation errors)",
      "description": "Happy path – valid JSON string, JSON input format, no customizer. All schema validators run and return an empty error set."
    },
    {
      "input": [
        { "parameter": "input", "value": "{\"name\":\"John\",\"age\":}" },
        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
        { "parameter": "executionCustomizer", "value": "null" }
      ],
      "expected": "Set<ValidationMessage> contains a parsing error message (e.g., \"Invalid JSON: Unexpected token\" )",
      "description": "Malformed JSON triggers a parsing exception inside `deserialize`; the method should catch it and return a ValidationMessage describing the JSON syntax error."
    },
    {
      "input": [
        { "parameter": "input", "value": "null" },
        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
        { "parameter": "executionCustomizer", "value": "null" }
      ],
      "expected": "IllegalArgumentException is thrown with message containing \"input must not be null\"",
      "description": "Branch where the `input` argument is null – the method validates arguments and throws IllegalArgumentException before any deserialization."
    },
    {
      "input": [
        { "parameter": "input", "value": "\"\"" },
        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
        { "parameter": "executionCustomizer", "value": "null" }
      ],
      "expected": "Set<ValidationMessage> contains a parsing error (empty JSON document)",
      "description": "Empty string is not a valid JSON document; `deserialize` fails and the method returns a parsing error message."
    },
    {
      "input": [
        { "parameter": "input", "value": "{\"name\":\"John\"}" },
        { "parameter": "inputFormat", "value": "null" },
        { "parameter": "executionCustomizer", "value": "null" }
      ],
      "expected": "IllegalArgumentException is thrown with message containing \"inputFormat must not be null\"",
      "description": "Branch where `inputFormat` argument is null – method should validate and throw IllegalArgumentException."
    },
    {
      "input": [
        { "parameter": "input", "value": "{\"age\":30}" },
        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
        { "parameter": "executionCustomizer", "value": "customizerEnableStrictMode" }
      ],
      "expected": "Set<ValidationMessage> contains a required‑property error for \"name\" (because customizer set strict mode)",
      "description": "Custom ExecutionContextCustomizer modifies the ExecutionContext (e.g., enables strict mode) causing a required‑property validator to fire. Verifies that the customizer is applied."
    },
    {
      "input": [
        { "parameter": "input", "value": "{\"name\":\"John\"}" },
        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
        { "parameter": "executionCustomizer", "value": "customizerThrowsRuntimeException" }
      ],
      "expected": "RuntimeException is propagated (the same exception thrown by the customizer)",
      "description": "Branch where the provided ExecutionContextCustomizer throws an unchecked exception; the method should not swallow it."
    },
    {
      "input": [
        { "parameter": "input", "value": "%7B%22name%22%3A%22John%22%7D" },
        { "parameter": "inputFormat", "value": "InputFormat.URL_ENCODED" },
        { "parameter": "executionCustomizer", "value": "null" }
      ],
      "expected": "Set<ValidationMessage> is empty (URL‑decoded JSON is valid)",
      "description": "When InputFormat is URL_ENCODED, `deserialize` uses URLDecoder; valid URL‑encoded JSON should be decoded correctly and pass validation."
    },
    {
      "input": [
        { "parameter": "input", "value": "%E0%A4%A" },
        { "parameter": "inputFormat", "value": "InputFormat.URL_ENCODED" },
        { "parameter": "executionCustomizer", "value": "null" }
      ],
      "expected": "UnsupportedEncodingException is thrown (invalid percent‑encoding)",
      "description": "Branch where URL decoding fails due to malformed percent‑encoding, causing `deserialize` to throw UnsupportedEncodingException which should be propagated."
    }
  ]
}