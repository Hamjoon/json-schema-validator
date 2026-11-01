{
  "testCases": [
    {
      "group": "Null node with valid executionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (no customizations)"
            },
            {
              "parameter": "node",
              "value": "null"
            }
          ],
          "expected": "ValidationResult with isValid() == false and a single ValidationMessage indicating that the instance node is null",
          "description": "When the node argument is null but a non‑null ExecutionContext is supplied the method should guard against NPE and return an invalid result immediately."
        }
      ]
    },
    {
      "group": "Enum validator – value not allowed",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "TextNode representing \"blue\""
            }
          ],
          "expected": "ValidationResult with isValid() == false and a ValidationMessage stating that the value \"blue\" is not one of the allowed enum values [\"red\",\"green\",\"yellow\"]",
          "description": "The schema contains an enum validator with allowed values [\"red\",\"green\",\"yellow\"]. Providing a value outside this set should trigger the enum validation branch."
        }
      ]
    },
    {
      "group": "Pattern validator – regex mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "TextNode representing \"123-AB\""
            }
          ],
          "expected": "ValidationResult with isValid() == false and a ValidationMessage indicating that the string \"123-AB\" does not match the pattern \"^[A-Z]{3}[0-9]{3}$\"",
          "description": "The schema defines a pattern validator expecting three uppercase letters followed by three digits. Supplying a non‑matching string should produce a pattern‑validation error."
        }
      ]
    },
    {
      "group": "$ref resolution – required property in referenced schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"name\":\"John\"}"
            }
          ],
          "expected": "ValidationResult with isValid() == false and a ValidationMessage mentioning missing required property \"age\" from the referenced schema",
          "description": "The primary schema contains a $ref to another schema that declares a required property \"age\". The instance node lacks this property, so the validator must resolve the reference and report the missing required field."
        }
      ]
    },
    {
      "group": "Combined required + type validators – type passes, required fails",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"optional\":\"value\"}"
            }
          ],
          "expected": "ValidationResult with isValid() == false and a ValidationMessage for missing required property \"mandatory\" (type validator succeeds because the node is an object)",
          "description": "The schema defines a required property \"mandatory\" and a type validator that expects an object. The instance is an object (type passes) but does not contain the required field, so only the required‑validator message should appear."
        }
      ]
    },
    {
      "group": "Array items type mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ArrayNode representing [\"a\", 2, \"c\"]"
            }
          ],
          "expected": "ValidationResult with isValid() == false and a ValidationMessage indicating that array element at index 1 is of type integer but expected string",
          "description": "The schema specifies an array where each item must be a string. The second element is an integer, causing the items‑type validator to add an error for that index."
        }
      ]
    },
    {
      "group": "AdditionalProperties false – extra field present",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"known\":\"value\",\"extra\":\"notAllowed\"}"
            }
          ],
          "expected": "ValidationResult with isValid() == false and a ValidationMessage stating that property \"extra\" is not allowed (additionalProperties = false)",
          "description": "The schema defines \"additionalProperties\": false. The instance contains an unexpected property, which should be reported as a validation error."
        }
      ]
    },
    {
      "group": "WalkListenerRunner – listener adds custom message without aborting",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with WalkListenerRunner that records a custom ValidationMessage \"listener‑info\" for each visited node"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"a\":1,\"b\":2}"
            }
          ],
          "expected": "ValidationResult with isValid() == true and a message set that includes the custom \"listener‑info\" messages (one per visited node) in addition to any schema‑generated messages (none in this case)",
          "description": "When a WalkListenerRunner is present but does not abort the walk, it may inject additional ValidationMessages. The method should collect those messages together with the normal validation results."
        }
      ]
    },
    {
      "group": "ExecutionContext with custom CollectorContext – messages are aggregated correctly",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext whose CollectorContext is pre‑populated with a custom scope that prefixes messages with \"[custom]\""
            },
            {
              "parameter": "node",
              "value": "IntNode representing 42"
            }
          ],
          "expected": "ValidationResult with isValid() == false and a ValidationMessage prefixed with \"[custom]\" describing the type mismatch (expected string, found integer)",
          "description": "The ExecutionContext can carry a CollectorContext that influences how messages are recorded. This test verifies that the custom scope is applied to the messages produced by the validator."
        }
      ]
    }
  ]
}