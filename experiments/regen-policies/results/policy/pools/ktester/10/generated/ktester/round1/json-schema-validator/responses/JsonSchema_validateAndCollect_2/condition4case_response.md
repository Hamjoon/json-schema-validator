{
  "testCases": [
    {
      "group": "Null or Missing Instance Node",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "null"
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage indicating that the instance node is null or missing",
          "description": "When the supplied JsonNode is null the method should short‑circuit and return a ValidationResult with an appropriate error message rather than attempting further validation."
        }
      ]
    },
    {
      "group": "Valid Type Validation (has TypeValidator, matching type)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"age\": 30}"
            }
          ],
          "expected": "ValidationResult with no ValidationMessage (empty set) because the node type matches the schema's TypeValidator",
          "description": "The schema contains a TypeValidator that expects an object. The supplied node is a JSON object, so type validation passes and no error messages are produced."
        }
      ]
    },
    {
      "group": "Invalid Type Validation (has TypeValidator, mismatching type)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"just a string\""
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage stating that the instance type is not allowed by the schema's TypeValidator",
          "description": "The schema expects an object (or another specific type) but the supplied node is a JSON string; the TypeValidator should generate a validation error."
        }
      ]
    },
    {
      "group": "Missing Required Property (has RequiredValidator)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{}"
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage that required property \"name\" is missing",
          "description": "The schema defines a required property \"name\". An empty object is supplied, so the RequiredValidator should add a missing‑property error."
        }
      ]
    },
    {
      "group": "All Required Properties Present",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"name\": \"John\", \"age\": 25}"
            }
          ],
          "expected": "ValidationResult with an empty ValidationMessage set because all required properties are present",
          "description": "The node satisfies the required properties of the schema, therefore no required‑property errors are emitted."
        }
      ]
    },
    {
      "group": "Recursive Anchor Handling (isRecursiveAnchor == true)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"self\": {\"$ref\": \"#\"}}"
            }
          ],
          "expected": "ValidationResult that does not enter infinite recursion and contains validation messages only for the outer object (e.g., no StackOverflowError)",
          "description": "When the schema is marked as a recursive anchor, the validator must recognise the `$ref` to itself and avoid endless recursion, returning a proper ValidationResult."
        }
      ]
    },
    {
      "group": "Keyword Walk Listener Invocation (keywordWalkListenerRunner != null)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"title\": \"Example\"}"
            }
          ],
          "expected": "ValidationResult where the mock WalkListenerRunner's `run` method has been called exactly once with the appropriate context",
          "description": "If a WalkListenerRunner is configured, `validateAndCollect` must invoke it during validation. The test verifies that the runner is called."
        }
      ]
    },
    {
      "group": "Schema Resource Root Shortcut (isSchemaResourceRoot == true)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"id\": \"http://example.com/schema\"}"
            }
          ],
          "expected": "ValidationResult that contains a ValidationMessage indicating that the schema resource root was used (or simply succeeds without extra processing)",
          "description": "When the schema is flagged as a resource root, the validator may apply special handling (e.g., base URI resolution). The test ensures this branch is exercised."
        }
      ]
    },
    {
      "group": "ExecutionContext Customizer Effects",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"value\": 10}"
            }
          ],
          "expected": "ValidationResult reflecting the modifications performed by a custom ExecutionContext (e.g., a custom format validator is added and triggers an extra ValidationMessage)",
          "description": "Passing a custom ExecutionContext (via the overloaded public method) can change validation behavior. This case verifies that the customizer’s effects are visible in the result."
        }
      ]
    }
  ]
}