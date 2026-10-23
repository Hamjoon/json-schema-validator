{
  "testCases": [
    {
      "group": "typeValidator present",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{ \"type\": \"string\" }"
            },
            {
              "parameter": "schemaPath",
              "value": "#"
            }
          ],
          "expected": "non‑null TypeValidator instance",
          "description": "Create a JsonSchema from a JSON node that defines a \"type\" keyword. After construction the internal `typeValidator` field should be instantiated, therefore `getTypeValidator()` must return a non‑null object."
        },
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{ \"type\": [\"string\", \"null\"] }"
            },
            {
              "parameter": "schemaPath",
              "value": "#/properties/name"
            }
          ],
          "expected": "non‑null TypeValidator instance handling composite types",
          "description": "When the schema contains a composite type (array of types), the validator still creates a `TypeValidator`. The test verifies that `getTypeValidator()` returns a non‑null instance even for multi‑type definitions."
        }
      ]
    },
    {
      "group": "typeValidator absent",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{ \"minimum\": 5 }"
            },
            {
              "parameter": "schemaPath",
              "value": "#/properties/age"
            }
          ],
          "expected": "null",
          "description": "Create a JsonSchema from a JSON node that does **not** contain a \"type\" keyword. The `typeValidator` field should remain `null`, so `getTypeValidator()` must return `null`."
        },
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{}"
            },
            {
              "parameter": "schemaPath",
              "value": "#"
            }
          ],
          "expected": "null",
          "description": "An empty schema does not define any type. The test confirms that `getTypeValidator()` returns `null` for an empty schema."
        }
      ]
    },
    {
      "group": "typeValidator after explicit initialization",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{ \"type\": \"integer\" }"
            },
            {
              "parameter": "schemaPath",
              "value": "#"
            }
          ],
          "expected": "non‑null TypeValidator instance",
          "description": "After constructing the schema, invoke `initializeValidators()` (if the implementation requires explicit initialization) and then call `getTypeValidator()`. The method should still return a non‑null validator."
        }
      ]
    }
  ]
}