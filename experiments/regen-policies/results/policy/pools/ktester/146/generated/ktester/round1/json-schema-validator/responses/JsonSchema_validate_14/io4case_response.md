{
  "newTestCases": [
    {
      "group": "Valid YAML input with successful validation",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "name: John\nage: 30" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>> (collects messages)" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "An empty list (no validation messages) is returned, indicating the YAML document conforms to the schema.",
          "description": "When a well‑formed YAML document that satisfies the schema is supplied, the validator should deserialize it correctly and return no validation errors."
        }
      ]
    },
    {
      "group": "Valid URL_ENCODED input that decodes to JSON and passes validation",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "%7B%22name%22%3A%22John%22%2C%22age%22%3A30%7D" },
            { "parameter": "inputFormat", "value": "InputFormat.URL_ENCODED" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>> (collects messages)" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "An empty list (no validation messages) is returned, indicating the URL‑encoded JSON was decoded and validated successfully.",
          "description": "The method should correctly URL‑decode the input, deserialize the resulting JSON, and perform validation without errors."
        }
      ]
    },
    {
      "group": "Valid JSON input that fails schema validation (missing required property)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>> (collects messages)" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "A list containing at least one ValidationMessage indicating that the required property \"age\" is missing.",
          "description": "The JSON is syntactically correct but does not satisfy the schema (required field absent); the validator must report the schema violation."
        }
      ]
    },
    {
      "group": "Valid JSON input that fails schema validation (type mismatch)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\",\"age\":\"thirty\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>> (collects messages)" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "A list containing a ValidationMessage describing a type error for property \"age\" (expected integer, got string).",
          "description": "The JSON conforms to syntax but violates the schema's type constraint; the validator must return an appropriate message."
        }
      ]
    },
    {
      "group": "Empty JSON string",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>> (collects messages)" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "A ValidationMessage describing a JSON parsing error (e.g., \"No content to map due to end-of-input\") is returned inside the list.",
          "description": "An empty string is not valid JSON; the deserialization step should fail and the error be reported as a validation message."
        }
      ]
    },
    {
      "group": "Whitespace‑only JSON string",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "   \t\n  " },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>> (collects messages)" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "A ValidationMessage indicating a parsing error due to lack of JSON content is returned.",
          "description": "String containing only whitespace cannot be parsed as JSON; the validator should surface the parsing problem."
        }
      ]
    },
    {
      "group": "Null InputFormat argument",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "null" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>> (collects messages)" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "An IllegalArgumentException (or NullPointerException) is thrown because the InputFormat argument is required.",
          "description": "Passing a null InputFormat should be rejected before any deserialization attempt."
        }
      ]
    },
    {
      "group": "OutputFormat that throws RuntimeException",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\",\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "OutputFormat that throws IllegalStateException when applied" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "The IllegalStateException propagates out of the validate method.",
          "description": "If the OutputFormat implementation throws an exception while converting the validation result, the validator must not swallow it."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer invoked (side‑effect verification)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\",\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>> (collects messages)" },
            { "parameter": "executionCustomizer", "value": "consumer that sets a mutable flag to true" }
          ],
          "expected": "An empty list is returned and after the call the external flag is true, proving the consumer was executed.",
          "description": "The method should invoke the provided Consumer with the ExecutionContext; side‑effects observable outside the method confirm the call."
        }
      ]
    },
    {
      "group": "Input with UTF‑8 BOM handling",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\uFEFF{\"name\":\"John\",\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "OutputFormat<List<ValidationMessage>> (collects messages)" },
            { "parameter": "executionCustomizer", "value": "consumer that does nothing" }
          ],
          "expected": "An empty list (no validation messages) is returned; the BOM is ignored during deserialization.",
          "description": "The validator should correctly handle a JSON string that starts with a UTF‑8 Byte Order Mark."
        }
      ]
    }
  ]
}