{
  "testCases": [
    {
      "group": "Valid JSON, validation enabled, customizer provided",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\",\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\", true)"
            }
          ],
          "expected": "ValidationResult with isValid=true and no ValidationMessage; executionCustomizer must be invoked (custom attribute set)",
          "description": "Ensures that a well‑formed JSON string is deserialized, the schema is validated, and the supplied Consumer is called."
        }
      ]
    },
    {
      "group": "Valid JSON, validation disabled, no customizer",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"Jane\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": false },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "ValidationResult with isValid=true (no validation performed) and empty messages; consumer not invoked",
          "description": "When validation flag is false the method should skip schema checks and simply return a successful result."
        }
      ]
    },
    {
      "group": "Malformed JSON triggers deserialization exception",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"Bob\", \"age\":}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "throws com.fasterxml.jackson.core.JsonParseException (or a wrapped ValidationException) and returns ValidationResult with isValid=false and a message indicating parsing error",
          "description": "The method must correctly propagate a parsing failure from the internal deserialize() call."
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
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "throws IllegalArgumentException with message containing 'input must not be null'",
          "description": "The method should guard against a null JSON payload before attempting deserialization."
        }
      ]
    },
    {
      "group": "Null InputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"Alice\"}" },
            { "parameter": "inputFormat", "value": null },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "throws IllegalArgumentException with message containing 'inputFormat must not be null'",
          "description": "The method must validate that a non‑null InputFormat is supplied."
        }
      ]
    },
    {
      "group": "Unsupported character encoding during deserialization",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "%E0%A4%A" },  
            { "parameter": "inputFormat", "value": "InputFormat.URL_ENCODED" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "throws UnsupportedEncodingException wrapped in ValidationException; ValidationResult.isValid=false",
          "description": "When URLDecoder fails due to malformed percent‑encoding, the method should surface the error."
        }
      ]
    },
    {
      "group": "Consumer throws runtime exception",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"id\":1}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }"
            }
          ],
          "expected": "throws RuntimeException with message 'customizer failure'; ValidationResult not returned",
          "description": "Verifies that an exception thrown by the supplied Consumer propagates out of the walk method."
        }
      ]
    }
  ]
}