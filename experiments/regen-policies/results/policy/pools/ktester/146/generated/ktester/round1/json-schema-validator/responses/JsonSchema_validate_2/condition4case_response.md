{
  "group": "validate(JsonNode) - basic delegation and error handling",
  "cases": [
    {
      "input": [
        {
          "parameter": "rootNode",
          "value": "null"
        }
      ],
      "expected": "NullPointerException",
      "description": "Passing a null JsonNode should result in a NullPointerException because the method delegates to validate(rootNode, OutputFormat.DEFAULT) which expects a non‑null node."
    },
    {
      "input": [
        {
          "parameter": "rootNode",
          "value": "{ \"name\": \"John\", \"age\": 30 }"
        }
      ],
      "expected": "empty Set<ValidationMessage>",
      "description": "A JSON document that fully satisfies the schema (e.g., required string and integer properties) should produce an empty set of validation messages."
    },
    {
      "input": [
        {
          "parameter": "rootNode",
          "value": "{ \"name\": 123, \"age\": \"thirty\" }"
        }
      ],
      "expected": "non‑empty Set<ValidationMessage> containing type‑mismatch messages for 'name' and 'age'",
      "description": "When the JSON contains values of the wrong type, the validator should return messages indicating the type mismatches."
    },
    {
      "input": [
        {
          "parameter": "rootNode",
          "value": "{ \"age\": 25 }"
        }
      ],
      "expected": "non‑empty Set<ValidationMessage> containing a required‑property missing message for 'name'",
      "description": "If a required property defined in the schema is missing, the validator must report a validation message for the missing property."
    },
    {
      "input": [
        {
          "parameter": "rootNode",
          "value": "{ \"$ref\": \"#/definitions/person\", \"name\": \"Alice\" }"
        }
      ],
      "expected": "empty Set<ValidationMessage> (reference resolved successfully)",
      "description": "A schema that contains a $ref to a valid definition should resolve the reference and validate successfully, resulting in no validation messages."
    },
    {
      "input": [
        {
          "parameter": "rootNode",
          "value": "{ \"$ref\": \"#/definitions/unknown\" }"
        }
      ],
      "expected": "ValidationMessage indicating unresolved $ref error",
      "description": "When a $ref points to a non‑existent definition, the validator should report an unresolved reference validation message."
    },
    {
      "input": [
        {
          "parameter": "rootNode",
          "value": "{ \"id\": \"#myAnchor\", \"$recursiveAnchor\": true, \"properties\": { \"self\": { \"$ref\": \"#myAnchor\" } } }"
        }
      ],
      "expected": "empty Set<ValidationMessage> (recursive anchor handled without infinite recursion)",
      "description": "A schema that declares a recursive anchor and references itself should be processed without causing a stack overflow, yielding no validation errors for a conforming instance."
    },
    {
      "input": [
        {
          "parameter": "rootNode",
          "value": "{ \"id\": \"#myAnchor\", \"$recursiveAnchor\": true, \"properties\": { \"self\": { \"$ref\": \"#myAnchor\" } } }"
        }
      ],
      "expected": "non‑empty Set<ValidationMessage> if the instance violates the recursive definition (e.g., missing required fields)",
      "description": "When the instance does not satisfy the recursively anchored schema, validation messages should be produced, confirming that recursion is correctly evaluated."
    }
  ]
}