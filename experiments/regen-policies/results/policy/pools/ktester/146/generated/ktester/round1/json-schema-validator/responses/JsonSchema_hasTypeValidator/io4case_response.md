{
  "tests": [
    {
      "group": "hasTypeValidator_withObjectType",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
            }
          ],
          "expected": true,
          "description": "A schema that defines a top‑level \"type\": \"object\" should cause hasTypeValidator() to return true."
        }
      ]
    },
    {
      "group": "hasTypeValidator_withArrayOfTypes",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{ \"type\": [\"string\", \"null\"] }"
            }
          ],
          "expected": true,
          "description": "When the \"type\" keyword is an array of allowed types, the schema still has a TypeValidator, so hasTypeValidator() returns true."
        }
      ]
    },
    {
      "group": "hasTypeValidator_anyOfWithoutTopLevelType",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{ \"anyOf\": [ { \"type\": \"string\" }, { \"type\": \"number\" } ] }"
            }
          ],
          "expected": false,
          "description": "A schema that only contains an \"anyOf\" with sub‑schemas that have types but no top‑level \"type\" keyword should return false."
        }
      ]
    },
    {
      "group": "hasTypeValidator_refToTypeSchema",
      "cases": [
        {
          "input": [
            {
              "parameter": "mainSchemaJson",
              "value": "{ \"$ref\": \"#/definitions/target\" , \"definitions\": { \"target\": { \"type\": \"array\", \"items\": { \"type\": \"integer\" } } } }"
            }
          ],
          "expected": true,
          "description": "A schema that $ref‑s to another definition which defines a \"type\" should resolve the reference and report true."
        }
      ]
    },
    {
      "group": "hasTypeValidator_refToNoTypeSchema",
      "cases": [
        {
          "input": [
            {
              "parameter": "mainSchemaJson",
              "value": "{ \"$ref\": \"#/definitions/target\" , \"definitions\": { \"target\": { \"properties\": { \"id\": { \"type\": \"string\" } } } } }"
            }
          ],
          "expected": false,
          "description": "A $ref that points to a schema without a top‑level \"type\" keyword should result in false."
        }
      ]
    },
    {
      "group": "hasTypeValidator_withEnumOnly",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{ \"enum\": [1, 2, 3] }"
            }
          ],
          "expected": false,
          "description": "A schema that only defines an \"enum\" (no \"type\") must return false."
        }
      ]
    },
    {
      "group": "hasTypeValidator_afterInitializeValidators",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{ \"properties\": { \"age\": { \"type\": \"integer\" } } }"
            }
          ],
          "expected": false,
          "description": "Even after calling initializeValidators() on a schema that lacks a top‑level \"type\", hasTypeValidator() remains false."
        },
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{ \"type\": \"boolean\", \"properties\": { \"flag\": { \"type\": \"boolean\" } } }"
            }
          ],
          "expected": true,
          "description": "After initializeValidators() on a schema that already has a top‑level \"type\", the method still returns true."
        }
      ]
    },
    {
      "group": "hasTypeValidator_typeNullValue",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{ \"type\": null }"
            }
          ],
          "expected": false,
          "description": "A schema where \"type\" is explicitly set to null should not create a TypeValidator and therefore returns false (no exception should be thrown)."
        }
      ]
    }
  ]
}