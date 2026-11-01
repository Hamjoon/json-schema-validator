{
  "testCases": [
    {
      "group": "Non‑root evaluationPath without parent",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")" },
            { "parameter": "evaluationPath", "value": "JsonNodePath.of(\"/properties/name\")" },
            { "parameter": "schemaNode", "value": "objectNode().put(\"type\", \"string\")" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance whose evaluationPath equals JsonNodePath.of(\"/properties/name\") and whose parent is null",
          "description": "When a non‑root evaluationPath is supplied the factory must honour the path even if no parent schema is given, allowing creation of isolated sub‑schemas."
        }
      ]
    },
    {
      "group": "Boolean true schema node (always valid)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/boolean-true.json\")" },
            { "parameter": "evaluationPath", "value": "JsonNodePath.ROOT" },
            { "parameter": "schemaNode", "value": "BooleanNode.TRUE" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance that validates any JSON instance (always passes)",
          "description": "A boolean true schema is a valid JSON‑Schema shortcut meaning “accept everything”. The factory should create a schema that never produces validation errors."
        }
      ]
    },
    {
      "group": "Boolean false schema node (always invalid)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/boolean-false.json\")" },
            { "parameter": "evaluationPath", "value": "JsonNodePath.ROOT" },
            { "parameter": "schemaNode", "value": "BooleanNode.FALSE" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance that rejects every JSON instance (always fails)",
          "description": "A boolean false schema is the opposite shortcut meaning “reject everything”. The factory must produce a schema that always yields a validation error."
        }
      ]
    },
    {
      "group": "Relative $ref within same document",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/with-defs.json\")" },
            { "parameter": "evaluationPath", "value": "JsonNodePath.ROOT" },
            { "parameter": "schemaNode", "value": "objectNode().put(\"$ref\", \"#/definitions/person\")" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance whose underlying schemaNode equals the definition at /definitions/person (type object with expected properties)",
          "description": "A $ref that points to a local definition must be resolved correctly without loading external resources."
        }
      ]
    },
    {
      "group": "Invalid $ref target (non‑existent definition)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/with-defs.json\")" },
            { "parameter": "evaluationPath", "value": "JsonNodePath.ROOT" },
            { "parameter": "schemaNode", "value": "objectNode().put(\"$ref\", \"#/definitions/missing\")" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchemaException (or IllegalArgumentException) with message containing 'Unable to resolve reference'",
          "description": "If a $ref cannot be resolved the factory must fail fast, propagating a clear exception."
        }
      ]
    },
    {
      "group": "SchemaLocation containing only a fragment",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"#/definitions/address\")" },
            { "parameter": "evaluationPath", "value": "JsonNodePath.ROOT" },
            { "parameter": "schemaNode", "value": "objectNode().put(\"type\", \"object\").set(\"properties\", objectNode().set(\"city\", objectNode().put(\"type\", \"string\")))" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance whose base URI is resolved to the fragment within the same document (no external loading)",
          "description": "When the SchemaLocation consists solely of a fragment the factory should treat it as a reference to a location inside the current schemaNode."
        }
      ]
    },
    {
      "group": "Schema with unknown keyword (should be ignored)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/unknown-key.json\")" },
            { "parameter": "evaluationPath", "value": "JsonNodePath.ROOT" },
            { "parameter": "schemaNode", "value": "objectNode().put(\"type\", \"string\").put(\"x‑custom\", \"value\")" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance that validates strings and silently ignores the unknown 'x‑custom' keyword",
          "description": "The factory must not reject schemas that contain non‑standard keywords; such keywords should be ignored according to the specification."
        }
      ]
    },
    {
      "group": "Schema with 'type' as an array (multiple allowed types)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/multi-type.json\")" },
            { "parameter": "evaluationPath", "value": "JsonNodePath.ROOT" },
            { "parameter": "schemaNode", "value": "objectNode().set(\"type\", arrayNode().add(\"string\").add(\"null\"))" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance that accepts either string values or null values",
          "description": "The 'type' keyword may be an array; the factory must create a validator that passes when the instance matches any listed type."
        }
      ]
    },
    {
      "group": "Schema with empty 'enum' array (invalid schema)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/empty-enum.json\")" },
            { "parameter": "evaluationPath", "value": "JsonNodePath.ROOT" },
            { "parameter": "schemaNode", "value": "objectNode().set(\"enum\", arrayNode())" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "IllegalArgumentException with message containing 'enum must contain at least one element'",
          "description": "An empty enum is prohibited by the JSON‑Schema spec; the factory should reject such a schema during creation."
        }
      ]
    },
    {
      "group": "Schema with 'const' set to null",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/const-null.json\")" },
            { "parameter": "evaluationPath", "value": "JsonNodePath.ROOT" },
            { "parameter": "schemaNode", "value": "objectNode().set(\"const\", NullNode.instance)" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance that validates only the literal JSON null value",
          "description": "The 'const' keyword may hold any JSON value, including null; the factory must create a validator that succeeds only for that exact value."
        }
      ]
    }
  ]
}