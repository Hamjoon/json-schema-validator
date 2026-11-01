{
  "groups": [
    {
      "group": "Valid input – satisfies schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"John Doe\" }"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors).",
          "description": "The schema requires a string property \"name\". Supplying a JSON object that contains a valid \"name\" field should pass validation with no errors."
        }
      ]
    },
    {
      "group": "Missing required property – should produce a validation error",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ }"
            }
          ],
          "expected": "A Set<ValidationMessage> containing one message whose code is \"required\" and whose message mentions the missing property \"name\".",
          "description": "The schema declares \"name\" as a required property. An empty object violates this rule, so the validator must return a validation message indicating the missing required field."
        }
      ]
    },
    {
      "group": "Type mismatch – root node is not an object",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "[1, 2, 3]"
            }
          ],
          "expected": "A Set<ValidationMessage> containing a message with code \"type\" indicating that the instance type (array) does not match the expected type (object).",
          "description": "The schema expects the root node to be an object. Providing a JSON array should trigger a type‑validation error."
        }
      ]
    },
    {
      "group": "Null root node – defensive handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            }
          ],
          "expected": "Throws NullPointerException (or returns a Set containing a single message with code \"null\" depending on implementation).",
          "description": "Calling validate(JsonNode) with a null argument tests the method’s null‑safety. The expected behavior is either an explicit NPE or a validation message indicating that the instance is null."
        }
      ]
    },
    {
      "group": "Schema with no validators – always succeeds",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"any string\""
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors).",
          "description": "When the JsonSchema instance is created from an empty schema (e.g., `{}`) it has no validators. Any JSON value, including a plain string, should validate successfully."
        }
      ]
    }
  ]
}