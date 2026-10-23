{
  "testCases": [
    {
      "group": "Validate schema flag true - valid node",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult with no errors (isSuccess() == true)",
          "description": "When shouldValidateSchema is true and the supplied JsonNode conforms to the schema (e.g., required properties present and types match), walk should return a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Validate schema flag true - invalid node (type mismatch)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"name\":123,\"age\":\"thirty\"}"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult containing type‑mismatch ValidationMessage(s) (isSuccess() == false)",
          "description": "When shouldValidateSchema is true and the node violates the schema (string expected for \"name\" but number provided, integer expected for \"age\" but string provided), walk must collect the corresponding validation errors."
        }
      ]
    },
    {
      "group": "Validate schema flag false - node processed without validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"unexpected\":\"value\"}"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": false
            }
          ],
          "expected": "ValidationResult with no errors regardless of schema mismatches (isSuccess() == true)",
          "description": "When shouldValidateSchema is false the walk method should bypass schema validation and simply traverse the node, therefore it must return a successful ValidationResult even if the node does not satisfy the schema."
        }
      ]
    },
    {
      "group": "Null node input",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": null
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult containing a single ValidationMessage indicating that the instance node is null (isSuccess() == false)",
          "description": "If the supplied JsonNode is null, walk should treat it as an invalid instance and return a ValidationResult with an appropriate error message."
        }
      ]
    },
    {
      "group": "Node missing required property",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"age\":25}"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult containing a required‑property ValidationMessage for \"name\" (isSuccess() == false)",
          "description": "When the schema defines a required property (e.g., \"name\") and the node does not contain it, walk must report a missing‑required‑property error."
        }
      ]
    },
    {
      "group": "Dynamic anchor handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"$anchor\":\"myAnchor\",\"value\":42}"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult with no errors if the dynamic anchor is resolved correctly; otherwise contains an error about unresolved anchor",
          "description": "If the schema contains a dynamic anchor, walk should resolve it during traversal. The test verifies that a valid node referencing a dynamic anchor yields success, while an unresolved anchor would produce an error."
        }
      ]
    },
    {
      "group": "Schema with $ref that cannot be resolved",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"$ref\":\"#/nonexistent\"}"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage about an unresolved $ref (isSuccess() == false)",
          "description": "When the schema contains a $ref that points to a non‑existent definition, walk must detect the failure to resolve the reference and report it as an error."
        }
      ]
    }
  ]
}