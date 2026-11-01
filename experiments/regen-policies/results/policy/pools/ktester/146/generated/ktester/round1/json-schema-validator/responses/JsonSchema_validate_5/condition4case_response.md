{
  "testCases": [
    {
      "group": "Successful validation – empty schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {} (empty JSON object)"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat that gathers ValidationMessage objects into a List"
            }
          ],
          "expected": "An empty List (no ValidationMessage) is returned by the OutputFormat",
          "description": "When the schema has no validators (e.g., an empty schema) the validate method should complete without adding any ValidationMessage to the OutputFormat."
        }
      ]
    },
    {
      "group": "Validation with type error",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"age\":\"twenty\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage with messageId \"type\" and path \"/age\"",
          "description": "The schema defines \"age\" as an integer. Supplying a string should trigger the TypeValidator branch and a corresponding ValidationMessage should be added to the OutputFormat."
        }
      ]
    },
    {
      "group": "Validation with custom validator failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage with messageId \"minLength\" for path \"/name\"",
          "description": "A custom LengthValidator (or MinLengthValidator) is attached to the schema. An empty string violates the minLength constraint, exercising the branch where a validator adds a ValidationMessage."
        }
      ]
    },
    {
      "group": "Null rootNode argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) thrown before any validation occurs",
          "description": "The validate method should guard against a null rootNode and throw an appropriate exception, covering the null‑check branch."
        }
      ]
    },
    {
      "group": "Null OutputFormat argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {}"
            },
            {
              "parameter": "format",
              "value": "null"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) thrown because OutputFormat is required",
          "description": "A null OutputFormat must be rejected, exercising the branch that validates the format argument."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer is null (default path)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"status\":true}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "Empty List – validation succeeds; the internal ExecutionContext is created with default settings",
          "description": "The public validate(JsonNode, OutputFormat) forwards to the three‑argument overload with a null ExecutionContextCustomizer. This test ensures that the null customizer branch does not cause a failure."
        }
      ]
    },
    {
      "group": "Recursive anchor handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing a recursive structure that references itself via $recursiveAnchor"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "Empty List – no ValidationMessage, confirming that the recursive‑anchor branch correctly avoids infinite loops",
          "description": "When the schema is marked as a recursive anchor, the validator must detect and handle recursion. This case forces the branch that checks `isRecursiveAnchor()`."
        }
      ]
    },
    {
      "group": "$ref resolution branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"$ref\":\"#/definitions/person\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing ValidationMessages defined in the referenced schema (e.g., missing required property \"name\")",
          "description": "The method must resolve a $ref and delegate validation to the referenced JsonSchema. This test covers the branch that calls `getRefSchema()` / `fromRef()`."
        }
      ]
    },
    {
      "group": "Schema with multiple validators – mixed success/failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"id\":1,\"email\":\"invalid-email\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage for the email format error, but no message for the valid integer id",
          "description": "The schema contains an integer validator for `id` and a pattern validator for `email`. Only the email validator should fail, exercising the loop that iterates over validators and adds messages conditionally."
        }
      ]
    }
  ]
}