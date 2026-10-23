{
  "testCases": [
    {
      "group": "Number type handling",
      "cases": [
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing integer value 7" },
            { "parameter": "schemaType", "value": "JsonType.NUMBER" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "default ValidationContext (strict)" }
          ],
          "expected": "true",
          "description": "An integer JSON node satisfies the NUMBER schema type because integers are a subset of numbers."
        },
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing floating point value 3.14" },
            { "parameter": "schemaType", "value": "JsonType.NUMBER" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "default ValidationContext (strict)" }
          ],
          "expected": "true",
          "description": "A floating‑point JSON node matches the NUMBER schema type."
        },
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing floating point value 2.718" },
            { "parameter": "schemaType", "value": "JsonType.INTEGER" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "default ValidationContext (strict)" }
          ],
          "expected": "false",
          "description": "A floating‑point node does not satisfy an INTEGER schema type under strict checking."
        }
      ]
    },
    {
      "group": "Boolean type handling",
      "cases": [
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing boolean true" },
            { "parameter": "schemaType", "value": "JsonType.BOOLEAN" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "default ValidationContext" }
          ],
          "expected": "true",
          "description": "A JSON boolean true matches the BOOLEAN schema type."
        },
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing boolean false" },
            { "parameter": "schemaType", "value": "JsonType.BOOLEAN" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "default ValidationContext" }
          ],
          "expected": "true",
          "description": "A JSON boolean false matches the BOOLEAN schema type."
        },
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing boolean true" },
            { "parameter": "schemaType", "value": "JsonType.STRING" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "default ValidationContext" }
          ],
          "expected": "false",
          "description": "A boolean node does not satisfy a STRING schema type."
        }
      ]
    },
    {
      "group": "Array type handling",
      "cases": [
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing empty array []" },
            { "parameter": "schemaType", "value": "JsonType.ARRAY" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "default ValidationContext" }
          ],
          "expected": "true",
          "description": "An empty JSON array matches the ARRAY schema type."
        },
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing array [1, \"two\", true]" },
            { "parameter": "schemaType", "value": "JsonType.ARRAY" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "default ValidationContext" }
          ],
          "expected": "true",
          "description": "A non‑empty JSON array also satisfies the ARRAY schema type regardless of its element types."
        },
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing array [1,2,3]" },
            { "parameter": "schemaType", "value": "JsonType.OBJECT" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "default ValidationContext" }
          ],
          "expected": "false",
          "description": "An array node does not satisfy an OBJECT schema type."
        }
      ]
    },
    {
      "group": "Object type handling",
      "cases": [
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing object {\"a\":1,\"b\":\"x\"}" },
            { "parameter": "schemaType", "value": "JsonType.OBJECT" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "default ValidationContext" }
          ],
          "expected": "true",
          "description": "A JSON object matches the OBJECT schema type."
        },
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing object {\"key\":\"value\"}" },
            { "parameter": "schemaType", "value": "JsonType.ARRAY" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "default ValidationContext" }
          ],
          "expected": "false",
          "description": "A JSON object does not satisfy an ARRAY schema type."
        }
      ]
    },
    {
      "group": "Parent schema without declared type",
      "cases": [
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing integer 12" },
            { "parameter": "schemaType", "value": "JsonType.INTEGER" },
            { "parameter": "parentSchema", "value": "JsonSchema with no explicit type (type = null)" },
            { "parameter": "validationContext", "value": "default ValidationContext" }
          ],
          "expected": "true",
          "description": "When the parent schema does not declare a type, the validator falls back to its own schemaType; the integer node matches INTEGER."
        }
      ]
    },
    {
      "group": "Null node with non‑NULL schema type",
      "cases": [
        {
          "input": [
            { "parameter": "node", "value": "null (Jackson NullNode)" },
            { "parameter": "schemaType", "value": "JsonType.BOOLEAN" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "default ValidationContext" }
          ],
          "expected": "false",
          "description": "A JSON null does not satisfy a BOOLEAN schema type."
        }
      ]
    },
    {
      "group": "Loose type checking does not accept floating point for INTEGER",
      "cases": [
        {
          "input": [
            { "parameter": "node", "value": "JsonNode representing floating point value 9.99" },
            { "parameter": "schemaType", "value": "JsonType.INTEGER" },
            { "parameter": "parentSchema", "value": "null" },
            { "parameter": "validationContext", "value": "ValidationContext with option \"typeLoose\" set to true" }
          ],
          "expected": "false",
          "description": "Even with \"typeLoose\" enabled, a floating‑point number should not be considered an INTEGER."
        }
      ]
    }
  ]
}