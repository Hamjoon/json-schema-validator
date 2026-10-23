{
  "testCases": [
    {
      "group": "Matching schema type",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "JsonNode representing integer value 42"
            },
            {
              "parameter": "schemaType",
              "value": "JsonType.INTEGER"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            },
            {
              "parameter": "validationContext",
              "value": "default ValidationContext"
            }
          ],
          "expected": "true",
          "description": "The JSON node is an integer and the validator's schema type is INTEGER, so equalsToSchemaType should return true."
        }
      ]
    },
    {
      "group": "Mismatching schema type",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "JsonNode representing string \"hello\""
            },
            {
              "parameter": "schemaType",
              "value": "JsonType.INTEGER"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            },
            {
              "parameter": "validationContext",
              "value": "default ValidationContext"
            }
          ],
          "expected": "false",
          "description": "The JSON node is a string while the validator expects an INTEGER, therefore equalsToSchemaType must return false."
        }
      ]
    },
    {
      "group": "Null JSON node",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "null (Jackson NullNode)"
            },
            {
              "parameter": "schemaType",
              "value": "JsonType.NULL"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            },
            {
              "parameter": "validationContext",
              "value": "default ValidationContext"
            }
          ],
          "expected": "true",
          "description": "When the node is a JSON null and the schema type is NULL, equalsToSchemaType should recognise the match and return true."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "null (Jackson NullNode)"
            },
            {
              "parameter": "schemaType",
              "value": "JsonType.STRING"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            },
            {
              "parameter": "validationContext",
              "value": "default ValidationContext"
            }
          ],
          "expected": "false",
          "description": "A JSON null does not satisfy a non‑NULL schema type (e.g., STRING), so the method must return false."
        }
      ]
    },
    {
      "group": "Undefined schema type (null schemaType)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "JsonNode representing boolean true"
            },
            {
              "parameter": "schemaType",
              "value": "null"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            },
            {
              "parameter": "validationContext",
              "value": "default ValidationContext"
            }
          ],
          "expected": "false",
          "description": "If the validator's schemaType is null, the method cannot perform a type comparison and must return false."
        }
      ]
    },
    {
      "group": "Parent schema overrides type",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "JsonNode representing string \"2021-01-01\""
            },
            {
              "parameter": "schemaType",
              "value": "JsonType.STRING"
            },
            {
              "parameter": "parentSchema",
              "value": "JsonSchema whose type is STRING and format is date"
            },
            {
              "parameter": "validationContext",
              "value": "default ValidationContext"
            }
          ],
          "expected": "true",
          "description": "When a parent schema is present, equalsToSchemaType must also consider the parent’s declared type. The node matches the parent’s STRING type, so the method returns true."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "JsonNode representing integer 10"
            },
            {
              "parameter": "schemaType",
              "value": "JsonType.INTEGER"
            },
            {
              "parameter": "parentSchema",
              "value": "JsonSchema whose type is STRING"
            },
            {
              "parameter": "validationContext",
              "value": "default ValidationContext"
            }
          ],
          "expected": "false",
          "description": "Even though the node matches the validator’s own schemaType (INTEGER), the parent schema forces a STRING type, causing a mismatch and the method must return false."
        }
      ]
    },
    {
      "group": "ValidationContext influences type checking",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "JsonNode representing integer 5"
            },
            {
              "parameter": "schemaType",
              "value": "JsonType.STRING"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            },
            {
              "parameter": "validationContext",
              "value": "ValidationContext with option \"typeLoose\" set to true"
            }
          ],
          "expected": "true",
          "description": "When the ValidationContext is configured for loose type checking, an integer node may be accepted for a STRING schema type, so equalsToSchemaType should return true."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "JsonNode representing integer 5"
            },
            {
              "parameter": "schemaType",
              "value": "JsonType.STRING"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            },
            {
              "parameter": "validationContext",
              "value": "default ValidationContext (strict type checking)"
            }
          ],
          "expected": "false",
          "description": "With strict type checking (default context), an integer node does not satisfy a STRING schema type, therefore the method must return false."
        }
      ]
    }
  ]
}