{
  "testCases": [
    {
      "group": "hasTypeValidator_realSchema",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonSchema",
              "value": "JsonSchema instance created via JsonSchemaFactory.fromJsonNode with schema node {\"type\":\"string\"}"
            }
          ],
          "expected": "true",
          "description": "A schema that explicitly defines a single type should provide a non‑null TypeValidator, therefore hasTypeValidator() must return true."
        },
        {
          "input": [
            {
              "parameter": "jsonSchema",
              "value": "JsonSchema instance created via JsonSchemaFactory.fromJsonNode with an empty schema node {}"
            }
          ],
          "expected": "false",
          "description": "An empty schema contains no \"type\" keyword, so getTypeValidator() returns null and hasTypeValidator() must return false."
        }
      ]
    },
    {
      "group": "hasTypeValidator_multipleTypes",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonSchema",
              "value": "JsonSchema instance created via JsonSchemaFactory.fromJsonNode with schema node {\"type\":[\"string\",\"null\"]}"
            }
          ],
          "expected": "true",
          "description": "When the schema defines a type array, the implementation creates a composite TypeValidator; hasTypeValidator() should therefore return true."
        }
      ]
    },
    {
      "group": "hasTypeValidator_otherValidatorsOnly",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonSchema",
              "value": "JsonSchema instance created via JsonSchemaFactory.fromJsonNode with schema node {\"maxLength\":5}"
            }
          ],
          "expected": "false",
          "description": "Presence of other validation keywords (e.g., maxLength) does not create a TypeValidator; hasTypeValidator() must still return false."
        }
      ]
    }
  ]
}