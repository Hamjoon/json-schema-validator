{
  "testCases": [
    {
      "group": "Combined Validators (required + type)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where JsonSchema.hasRequiredValidator() and hasTypeValidator() both return true; RequiredValidator expects property \"id\"; TypeValidator expects type integer for property \"id\""
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"id\": 123 }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and no ValidationMessage because both required and type constraints are satisfied.",
          "description": "When a schema defines both a required property and a type constraint on that property, the method should invoke both validators and succeed if the node meets all constraints."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where JsonSchema.hasRequiredValidator() and hasTypeValidator() both return true; RequiredValidator expects property \"age\"; TypeValidator expects type integer for property \"age\""
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"age\": \"twenty\" }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and two ValidationMessages: one for missing required property (if any) and one for type mismatch on \"age\".",
          "description": "Both validators should be executed; the type validator fails because the value is a string, and the required validator passes because the property exists. The result contains the type‑mismatch message."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where JsonSchema.hasRequiredValidator() returns true (requires \"name\") and hasTypeValidator() returns true (expects string for \"name\")"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and two ValidationMessages: one for missing required property \"name\" and another for type mismatch (type validator may also report missing type).",
          "description": "Both validators fail: the required validator reports the missing property, and the type validator reports that the (non‑existent) value does not match the expected type."
        }
      ]
    },
    {
      "group": "Schema Composition – allOf / anyOf / oneOf",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema contains an allOf array of two subschemas: one requiring property \"a\" (type string), another requiring property \"b\" (type integer)"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"a\": \"foo\", \"b\": 10 }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and empty ValidationMessage set because node satisfies all subschemas.",
          "description": "allOf should cause validation against each subschema; success when all subschemas validate."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema contains an anyOf array of two subschemas: first requires property \"x\" (type boolean), second requires property \"y\" (type number)"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"y\": 3.14 }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and empty ValidationMessage set because node matches at least one subschema.",
          "description": "anyOf should succeed when any one subschema validates; here only the second subschema matches."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema contains a oneOf array of three subschemas: (1) requires \"type\":\"A\", (2) requires \"type\":\"B\", (3) requires \"type\":\"C\""
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"type\": \"A\" }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and empty ValidationMessage set because exactly one subschema matches.",
          "description": "oneOf should succeed only when a single subschema matches; this case verifies the happy path."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema contains a oneOf array of two subschemas that both match the node"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"common\": true }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage indicating that more than one oneOf subschema matched.",
          "description": "oneOf must fail when more than one subschema validates; the method should aggregate the appropriate error."
        }
      ]
    },
    {
      "group": "Recursive $ref and Circular Reference Detection",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where JsonSchema.getRefSchemaNode(\"#/definitions/node\") returns a schema that itself contains a $ref back to \"#/definitions/node\" (direct circular reference)"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"value\": 1 }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage indicating a circular $ref resolution error (e.g., \"Circular reference detected at #/definitions/node\").",
          "description": "The method must detect circular $ref chains and abort validation with a clear error rather than infinite recursion or stack overflow."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where $ref resolves through multiple levels (A → B → C) before reaching a leaf schema without further $ref"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"leaf\": \"ok\" }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and empty ValidationMessage set, confirming that multi‑level $ref resolution works correctly.",
          "description": "Ensures that the method can follow a chain of $ref pointers across several schemas and still validate successfully."
        }
      ]
    },
    {
      "group": "Discriminator Context Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema contains a discriminator with mapping { \"cat\": \"#/components/schemas/Cat\", \"dog\": \"#/components/schemas/Dog\" } and the current node has property \"type\":\"cat\""
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"type\": \"cat\", \"meow\": true }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and empty ValidationMessage set because the discriminator resolved to the Cat schema and the node satisfied it.",
          "description": "Validates that the discriminator logic is applied, the correct subschema is selected, and validation proceeds against that subschema."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where discriminator mapping does not contain the value present in the node (e.g., node has \"type\":\"bird\")"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"type\": \"bird\", \"fly\": true }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage indicating that the discriminator value \"bird\" could not be resolved to any schema.",
          "description": "Ensures that an unknown discriminator value results in a validation error."
        }
      ]
    },
    {
      "group": "Schema $id and Base URI Resolution",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where the root schema has $id \"http://example.com/schemas/base.json\" and contains a subschema with $ref \"other.json#/def\""
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"refProp\": \"value\" }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true (assuming the referenced schema at http://example.com/schemas/other.json#/def validates the node) and empty ValidationMessage set.",
          "description": "Verifies that relative $ref URIs are resolved against the base URI defined by $id."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where $id contains an encoded component (e.g., \"http://example.com/schemas/%7Euser.json\") and a $ref uses the decoded form \"~/user.json#/def\""
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"decoded\": true }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and empty ValidationMessage set, confirming that URL decoding is performed correctly during $ref resolution.",
          "description": "Tests that URLDecoder handling of encoded characters in $id/$ref works as expected."
        }
      ]
    },
    {
      "group": "PatternProperties and AdditionalProperties",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema defines patternProperties { \"^foo_.*$\": { \"type\": \"string\" } } and additionalProperties set to false"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"foo_bar\": \"text\", \"foo_123\": \"more\", \"extra\": 5 }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage indicating that property \"extra\" is not allowed (additionalProperties false).",
          "description": "Ensures that patternProperties are evaluated correctly and that disallowed extra properties trigger an error."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema defines patternProperties { \"^num_.*$\": { \"type\": \"integer\" } } and additionalProperties set to true"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"num_one\": 1, \"num_two\": \"two\" }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage for property \"num_two\" indicating type mismatch, while \"num_one\" passes.",
          "description": "Validates that patternProperties are applied to matching keys and type errors are reported, even when additionalProperties is allowed."
        }
      ]
    },
    {
      "group": "Array Items Validation (tuple vs. list validation)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema defines \"items\" as an array of two subschemas: first expects type string, second expects type integer"
            },
            {
              "parameter": "node",
              "value": "ArrayNode representing [ \"a\", 10 ]"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and empty ValidationMessage set (tuple validation succeeds).",
          "description": "Tests tuple validation where each position has its own schema."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema defines \"items\" as an array of two subschemas (tuple) and \"additionalItems\" set to false"
            },
            {
              "parameter": "node",
              "value": "ArrayNode representing [ \"a\", 10, true ]"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage indicating that additional items are not allowed.",
          "description": "Ensures that additionalItems:false blocks extra array elements beyond the tuple definition."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema defines \"items\" as a single subschema (type integer) and \"minItems\":2, \"maxItems\":4"
            },
            {
              "parameter": "node",
              "value": "ArrayNode representing [ 1, 2, 3, 4, 5 ]"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage indicating that the array has more items than maxItems (5 > 4).",
          "description": "Validates length constraints together with homogeneous items validation."
        }
      ]
    },
    {
      "group": "Enum and Const Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema defines \"enum\": [ \"red\", \"green\", \"blue\" ]"
            },
            {
              "parameter": "node",
              "value": "TextNode representing \"green\""
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and empty ValidationMessage set.",
          "description": "Enum validation should succeed when the node value matches one of the allowed values."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema defines \"enum\": [ 1, 2, 3 ]"
            },
            {
              "parameter": "node",
              "value": "IntNode representing 5"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage indicating that the value is not in the enum list.",
          "description": "Enum validation should fail for values not listed."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema defines \"const\": { \"a\": 1, \"b\": 2 }"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"a\": 1, \"b\": 2 }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and empty ValidationMessage set.",
          "description": "Const validation should succeed when the node exactly matches the constant value."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where schema defines \"const\": \"fixedString\""
            },
            {
              "parameter": "node",
              "value": "TextNode representing \"differentString\""
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage indicating const mismatch.",
          "description": "Const validation should fail when the node differs from the constant."
        }
      ]
    },
    {
      "group": "Format Validation with Locale‑Specific Messages",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext with ValidationContext locale set to French and schema defines \"format\": \"email\""
            },
            {
              "parameter": "node",
              "value": "TextNode representing \"user@example.com\""
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and empty ValidationMessage set.",
          "description": "Valid email should pass regardless of locale; ensures that locale does not affect successful validation."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext with ValidationContext locale set to French and schema defines \"format\": \"email\""
            },
            {
              "parameter": "node",
              "value": "TextNode representing \"not-an-email\""
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage whose message text is French (e.g., \"doit être un courriel valide\").",
          "description": "Checks that format validation failures are reported using the locale‑specific message bundle."
        }
      ]
    },
    {
      "group": "CollectorContext Scope Propagation",
      "$cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where JsonSchema.getValidationContext().getCollectorContext().pushScope(\"/properties/inner\") is invoked before validation of a nested object"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"inner\": { \"value\": 1 } }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and empty ValidationMessage set; after validation the CollectorContext scope stack is restored to its previous state.",
          "description": "Ensures that the method correctly pushes and pops scopes in CollectorContext, allowing error messages to contain the proper JSON pointer."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext where a validation error occurs inside a nested scope; the CollectorContext should retain the pointer to the failing location (e.g., \"/inner/value\")."
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"inner\": { \"value\": \"string\" } } with a type validator expecting integer for \"value\""
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage whose path is \"/inner/value\".",
          "description": "Verifies that the scope information is correctly reflected in the resulting ValidationMessage."
        }
      ]
    },
    {
      "group": "Invalid JSON Node Types (MissingNode, NullNode)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext with a required validator for property \"name\""
            },
            {
              "parameter": "node",
              "value": "MissingNode instance (represents a missing property in parent object)"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage indicating that required property \"name\" is missing.",
          "description": "MissingNode should be treated as absent data; required validation must fail."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext with a type validator expecting \"null\" type"
            },
            {
              "parameter": "node",
              "value": "NullNode instance"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and empty ValidationMessage set.",
          "description": "NullNode should satisfy a schema that explicitly allows the null type."
        }
      ]
    },
    {
      "group": "Walk Listener Exception Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext with a WalkListenerRunner whose beforeWalk callback throws a RuntimeException"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing { \"any\": 1 }"
            }
          ],
          "expected": "ValidationResult with isSuccess() == false and a ValidationMessage indicating that a walk listener error occurred (or the exception propagates, depending on implementation).",
          "description": "Ensures that unexpected exceptions from walk listeners are captured and reported rather than causing the whole validation process to crash silently."
        }
      ]
    }
  ]
}