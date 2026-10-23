{
  "testCases": [
    {
      "group": "hasRequiredValidator_initialization",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{\"type\":\"object\",\"required\":[\"name\"]}"
            }
          ],
          "expected": true,
          "description": "A schema that defines an object type and contains a non‑empty \"required\" array should cause JsonSchema.initializeValidators() to create a RequiredValidator, therefore hasRequiredValidator() must return true."
        },
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{\"type\":\"object\"}"
            }
          ],
          "expected": false,
          "description": "When the schema is an object but does not declare a \"required\" keyword, initializeValidators() must not create a RequiredValidator, so hasRequiredValidator() should return false."
        },
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{\"type\":\"string\",\"required\":[\"len\"]}"
            }
          ],
          "expected": false,
          "description": "Even if a \"required\" keyword is present, it is only meaningful for object types. For a non‑object type (e.g., string) the RequiredValidator should not be instantiated, resulting in false."
        },
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{\"type\":\"object\",\"required\":[\"id\"]}"
            },
            {
              "parameter": "resetRequiredValidator",
              "value": true
            }
          ],
          "expected": false,
          "description": "After a schema that initially creates a RequiredValidator (hasRequiredValidator() = true), the private requiredValidator field is set back to null via reflection (resetRequiredValidator = true). The method must now return false."
        },
        {
          "input": [
            {
              "parameter": "parentSchemaJson",
              "value": "{\"type\":\"object\",\"required\":[\"parentProp\"]}"
            },
            {
              "parameter": "childSchemaJson",
              "value": "{\"type\":\"object\"}"
            }
          ],
          "expected": false,
          "description": "A child schema created with createChildSchema() that does not define its own \"required\" keyword must not inherit the parent’s RequiredValidator. Hence, hasRequiredValidator() on the child should be false."
        },
        {
          "input": [
            {
              "parameter": "parentSchemaJson",
              "value": "{\"type\":\"object\",\"required\":[\"parentProp\"]}"
            },
            {
              "parameter": "childSchemaJson",
              "value": "{\"type\":\"object\",\"required\":[\"childProp\"]}"
            }
          ],
          "expected": true,
          "description": "Both parent and child schemas define a \"required\" keyword. After creating the child schema via createChildSchema() and initializing its validators, the child must have its own RequiredValidator, so hasRequiredValidator() returns true."
        }
      ]
    }
  ]
}