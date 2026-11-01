{
  "test_cases": [
    {
      "group": "Invalid $ref Resolution",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing any valid JSON (e.g., {\"dummy\":1})"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<List<ValidationMessage>> that simply returns the list"
            }
          ],
          "expected": "JsonSchemaException",
          "description": "The schema associated with the JsonSchema instance contains a $ref that points to a non‑existent or malformed external schema file. During validation the library attempts to resolve the reference and throws a JsonSchemaException. The test verifies that the exception propagates out of validate(JsonNode, OutputFormat<T>)."
        }
      ]
    },
    {
      "group": "Unsupported Keyword Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing any JSON (e.g., {\"foo\":\"bar\"})"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<Set<ValidationMessage>> that collects messages into a Set"
            }
          ],
          "expected": "JsonSchemaException",
          "description": "The loaded schema contains an unknown/unsupported keyword (e.g., \"unknownKeyword\": true). When the validator factory tries to create a validator for this keyword it throws a JsonSchemaException. The test ensures that validate propagates this exception."
        }
      ]
    },
    {
      "group": "OutputFormat Throws Checked Exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing JSON that passes validation (e.g., {\"name\":\"John\"})"
            },
            {
              "parameter": "format",
              "value": "Custom OutputFormat<T> whose apply method throws java.io.IOException"
            }
          ],
          "expected": "RuntimeException (cause: IOException)",
          "description": "Even though the JSON instance is valid, the provided OutputFormat throws a checked IOException while processing the validation messages. The validate method should not swallow the exception; it must propagate it wrapped in a RuntimeException (as the method signature does not allow checked exceptions)."
        }
      ]
    },
    {
      "group": "Schema Not Initialized",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing any JSON (e.g., {\"any\":true})"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<List<ValidationMessage>> that returns the list"
            }
          ],
          "expected": "IllegalStateException",
          "description": "A JsonSchema instance is created via the protected constructor or reflection without invoking the internal initialization (validators are null). Calling validate on such an un‑initialized schema should throw IllegalStateException indicating that the schema has not been prepared for validation."
        }
      ]
    },
    {
      "group": "Recursive Anchor StackOverflow Protection",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing a JSON structure that exceeds the library's internal recursion limit (e.g., 5000 nested objects with a \"node\" property)"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<List<ValidationMessage>> that returns the list"
            }
          ],
          "expected": "StackOverflowError",
          "description": "When the instance depth goes beyond the safe recursion threshold the validator deliberately throws a StackOverflowError (instead of converting it to a ValidationMessage). The test confirms that this error is not caught inside validate and propagates to the caller."
        }
      ]
    }
  ]
}