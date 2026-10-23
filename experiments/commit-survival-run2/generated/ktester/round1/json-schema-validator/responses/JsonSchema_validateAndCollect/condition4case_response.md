{
  "testCases": [
    {
      "group": "Null and Invalid Parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "null"
            }
          ],
          "expected": "ValidationResult with a single ValidationMessage indicating that both executionContext and node must not be null (or a NullPointerException is thrown, depending on implementation).",
          "description": "Verify that the method defensively handles a completely null input and reports the problem rather than crashing."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "ValidationResult with a ValidationMessage about missing ExecutionContext (or NPE).",
          "description": "Check behaviour when the ExecutionContext is null but a valid JsonNode is supplied."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "valid ExecutionContext mock"
            },
            {
              "parameter": "node",
              "value": "null"
            }
          ],
          "expected": "ValidationResult with a ValidationMessage about missing JsonNode (or NPE).",
          "description": "Check behaviour when the JsonNode argument is null while the ExecutionContext is valid."
        }
      ]
    },
    {
      "group": "Valid Schema – No Validators Triggered (happy path)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext for a schema that has no required or type validators"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and an empty set of ValidationMessage.",
          "description": "When the schema does not define any constraints, validation should succeed and return an empty result."
        }
      ]
    },
    {
      "group": "Required Validator Branches",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where JsonSchema.hasRequiredValidator() returns true and RequiredValidator validates successfully"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"requiredProp\": \"value\" }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and no ValidationMessage.",
          "description": "The required validator should be invoked and pass because the required property is present."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where JsonSchema.hasRequiredValidator() returns true and RequiredValidator fails"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage indicating the missing required property.",
          "description": "The required validator should be invoked and produce an error when the required field is absent."
        }
      ]
    },
    {
      "group": "Type Validator Branches",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where JsonSchema.hasTypeValidator() returns true and TypeValidator validates successfully"
            },
            {
              "parameter": "node",
              "value": "IntNode representing 42"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and no ValidationMessage.",
          "description": "The type validator should be invoked and succeed when the node type matches the schema type."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where JsonSchema.hasTypeValidator() returns true and TypeValidator fails"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage indicating a type mismatch.",
          "description": "The type validator should be invoked and report a type error when the node type does not match the schema."
        }
      ]
    },
    {
      "group": "Dynamic Anchor Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where JsonSchema.isDynamicAnchor() returns true"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "ValidationResult where the dynamic anchor is resolved (no exception) and validation proceeds normally.",
          "description": "When the schema is marked as a dynamic anchor, the method must resolve it before validation; this case checks that the resolution path is taken."
        }
      ]
    },
    {
      "group": "$ref Resolution Branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where JsonSchema.getRefSchemaNode(ref) returns a non‑null JsonNode"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"refProp\": \"value\" }"
            }
          ],
          "expected": "ValidationResult with validation performed against the referenced schema and appropriate success/failure messages.",
          "description": "If the schema contains a $ref, the method should retrieve the referenced schema node and validate against it; this case ensures that branch is exercised."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where JsonSchema.getRefSchemaNode(ref) returns null"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "ValidationResult with a ValidationMessage indicating that the $ref could not be resolved.",
          "description": "When a $ref cannot be resolved, validation should fail with an appropriate message."
        }
      ]
    },
    {
      "group": "Walk Listener Invocation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext with a non‑null WalkListenerRunner attached to the schema"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"a\": 1 }"
            }
          ],
          "expected": "ValidationResult where the WalkListenerRunner's before/after callbacks are invoked (verified via a mock) and validation result reflects the schema constraints.",
          "description": "Ensures that when a WalkListenerRunner is present, the method walks the schema tree and triggers the listener callbacks."
        }
      ]
    },
    {
      "group": "Root vs. Non‑Root Invocation (atRoot flag)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where atRoot() returns true"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "ValidationResult with normal validation flow; any root‑specific handling (e.g., $id resolution) is executed.",
          "description": "When the method is called at the root of the document, the atRoot flag is true; this case validates that branch."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where atRoot() returns false"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "ValidationResult with normal validation flow but without root‑only processing.",
          "description": "When the method is invoked for a nested node, the atRoot flag is false; this case checks that the non‑root branch is exercised."
        }
      ]
    }
  ]
}