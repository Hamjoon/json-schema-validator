{
  "testCases": [
    {
      "group": "primitive type validators",
      "cases": [
        {
          "input": [
            { "parameter": "schemaJson", "value": "{ \"type\": \"string\" }" },
            { "parameter": "schemaPath", "value": "#/properties/name" }
          ],
          "expected": "non‑null TypeValidator instance",
          "description": "Schema defines a primitive \"string\" type. `getTypeValidator()` must return a non‑null validator."
        },
        {
          "input": [
            { "parameter": "schemaJson", "value": "{ \"type\": \"number\" }" },
            { "parameter": "schemaPath", "value": "#/properties/price" }
          ],
          "expected": "non‑null TypeValidator instance",
          "description": "Schema defines a primitive \"number\" type. `getTypeValidator()` must return a non‑null validator."
        },
        {
          "input": [
            { "parameter": "schemaJson", "value": "{ \"type\": \"integer\" }" },
            { "parameter": "schemaPath", "value": "#/properties/count" }
          ],
          "expected": "non‑null TypeValidator instance",
          "description": "Schema defines a primitive \"integer\" type. `getTypeValidator()` must return a non‑null validator."
        },
        {
          "input": [
            { "parameter": "schemaJson", "value": "{ \"type\": \"boolean\" }" },
            { "parameter": "schemaPath", "value": "#/properties/active" }
          ],
          "expected": "non‑null TypeValidator instance",
          "description": "Schema defines a primitive \"boolean\" type. `getTypeValidator()` must return a non‑null validator."
        },
        {
          "input": [
            { "parameter": "schemaJson", "value": "{ \"type\": \"object\" }" },
            { "parameter": "schemaPath", "value": "#/properties/data" }
          ],
          "expected": "non‑null TypeValidator instance",
          "description": "Schema defines a primitive \"object\" type. `getTypeValidator()` must return a non‑null validator."
        },
        {
          "input": [
            { "parameter": "schemaJson", "value": "{ \"type\": \"array\" }" },
            { "parameter": "schemaPath", "value": "#/properties/list" }
          ],
          "expected": "non‑null TypeValidator instance",
          "description": "Schema defines a primitive \"array\" type. `getTypeValidator()` must return a non‑null validator."
        },
        {
          "input": [
            { "parameter": "schemaJson", "value": "{ \"type\": \"null\" }" },
            { "parameter": "schemaPath", "value": "#/properties/nullable" }
          ],
          "expected": "non‑null TypeValidator instance",
          "description": "Schema defines a primitive \"null\" type. `getTypeValidator()` must return a non‑null validator."
        }
      ]
    },
    {
      "group": "empty type array handling",
      "cases": [
        {
          "input": [
            { "parameter": "schemaJson", "value": "{ \"type\": [] }" },
            { "parameter": "schemaPath", "value": "#/properties/invalid" }
          ],
          "expected": "null",
          "description": "Schema contains an empty array for the \"type\" keyword, which is not a valid type definition. The implementation should leave `typeValidator` unset, so `getTypeValidator()` returns null."
        }
      ]
    },
    {
      "group": "repeated validator initialization",
      "cases": [
        {
          "input": [
            { "parameter": "schemaJson", "value": "{ \"type\": \"string\" }" },
            { "parameter": "schemaPath", "value": "#/properties/name" }
          ],
          "expected": "non‑null TypeValidator instance",
          "description": "After constructing the schema, call `initializeValidators()` twice. The second call must not replace the existing `typeValidator`; `getTypeValidator()` should still return a non‑null instance."
        }
      ]
    },
    {
      "group": "consistent instance across calls",
      "cases": [
        {
          "input": [
            { "parameter": "schemaJson", "value": "{ \"type\": \"integer\" }" },
            { "parameter": "schemaPath", "value": "#/properties/id" }
          ],
          "expected": "same instance on successive calls",
          "description": "Invoke `getTypeValidator()` multiple times on the same `JsonSchema` instance. All calls must return the identical `TypeValidator` object (reference equality)."
        }
      ]
    },
    {
      "group": "type validator with additional constraints",
      "cases": [
        {
          "input": [
            { "parameter": "schemaJson", "value": "{ \"type\": \"string\", \"minLength\": 3, \"enum\": [\"abc\", \"def\"] }" },
            { "parameter": "schemaPath", "value": "#/properties/code" }
          ],
          "expected": "non‑null TypeValidator instance",
          "description": "Schema defines a \"type\" together with other validation keywords (minLength, enum). Presence of extra constraints must not prevent creation of the `TypeValidator`."
        }
      ]
    },
    {
      "group": "type validator via $ref resolution",
      "cases": [
        {
          "input": [
            { "parameter": "schemaJson", "value": "{ \"$ref\": \"#/definitions/positiveInteger\" , \"definitions\": { \"positiveInteger\": { \"type\": \"integer\", \"minimum\": 1 } } }" },
            { "parameter": "schemaPath", "value": "#/properties/count" }
          ],
          "expected": "non‑null TypeValidator instance after $ref resolution",
          "description": "Schema uses a `$ref` that resolves to a definition containing a \"type\" keyword. After resolution, `getTypeValidator()` must return a non‑null validator."
        }
      ]
    }
  ]
}