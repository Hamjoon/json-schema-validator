{
  "testCases": [
    {
      "group": "Complex schema node structures - nested properties and required",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/nested.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$.person\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"type\":\"object\",\"properties\":{\"address\":{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"},\"zip\":{\"type\":\"string\"}}},\"name\":{\"type\":\"string\"}},\"required\":[\"address\",\"name\"]})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance containing ObjectValidator with required fields and nested ObjectValidator for 'address'",
          "description": "Verifies that a schema with nested object properties and a required array is processed correctly and the resulting JsonSchema contains the appropriate validators for the hierarchy."
        }
      ]
    },
    {
      "group": "Array type with items schema",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/arrayItems.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"type\":\"array\",\"items\":{\"type\":\"integer\",\"minimum\":0}})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance containing ArrayValidator with an ItemValidator for integer type",
          "description": "Ensures that an \"array\" type with an \"items\" object is translated into an ArrayValidator and that the item validator reflects the supplied integer constraints."
        }
      ]
    },
    {
      "group": "Boolean schema node handling",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/booleanTrue.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "BooleanNode.TRUE" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance that always validates (no validators attached)",
          "description": "A schema node that is the boolean literal true should produce a JsonSchema that accepts any instance."
        },
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/booleanFalse.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "BooleanNode.FALSE" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance that always fails validation (Contains a FalseValidator)",
          "description": "A schema node that is the boolean literal false should produce a JsonSchema that rejects any instance."
        }
      ]
    },
    {
      "group": "$id and $schema keywords handling",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/overrideId.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"$id\":\"http://example.com/custom-schema\",\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"string\"})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance whose internal schemaLocation is resolved to http://example.com/custom-schema",
          "description": "Tests that a schema containing a \"$id\" overrides the supplied SchemaLocation and that the \"$schema\" keyword selects the appropriate meta‑schema version."
        }
      ]
    },
    {
      "group": "Invalid top‑level schema node types",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/arrayNode.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "arrayNode([{\"type\":\"string\"}])" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "IllegalArgumentException",
          "description": "A top‑level array node is not a valid JSON Schema object; the factory should reject it with an IllegalArgumentException."
        },
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/emptyObject.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance with no validators (accepts any instance)",
          "description": "An empty object is a valid schema that imposes no constraints; the factory should create a JsonSchema that validates everything."
        }
      ]
    },
    {
      "group": "SchemaLocation edge cases",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/base.json#/definitions/foo\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"type\":\"number\"})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance whose base URI is classpath:/schemas/base.json and fragment is #/definitions/foo",
          "description": "Ensures that a SchemaLocation containing a fragment is preserved and used as the base for relative $ref resolution."
        },
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"ht!tp:// malformed-uri\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"type\":\"string\"})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "IllegalArgumentException",
          "description": "A malformed URI in SchemaLocation should cause the factory to reject the input with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Parent schema linking with pre‑existing validators",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/child.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$.child\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"type\":\"string\",\"minLength\":3})" },
            { "parameter": "parentSchema", "value": "new JsonSchemaFactory().getSchema(\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"parentProp\\\":{\\\"type\\\":\\\"integer\\\"}}}\")" }
          ],
          "expected": "JsonSchema instance whose parent reference points to the supplied parent schema and that contains a StringValidator with minLength=3",
          "description": "Confirms that when a non‑null parentSchema is supplied, the created schema retains a reference to that parent while still applying its own validators."
        }
      ]
    },
    {
      "group": "Enum keyword with empty array",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/emptyEnum.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"enum\":[]})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance containing EnumValidator with empty allowed values (validation will always fail)",
          "description": "Tests that an empty \"enum\" array is still recognized and results in an EnumValidator, even though it makes the schema unsatisfiable."
        }
      ]
    },
    {
      "group": "OneOf and AnyOf validator branches",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/oneOf.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"oneOf\":[{\"type\":\"string\"},{\"type\":\"integer\"}]})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance containing OneOfValidator",
          "description": "Verifies that a schema with a \"oneOf\" array triggers the creation of a OneOfValidator."
        },
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/anyOf.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"anyOf\":[{\"type\":\"boolean\"},{\"type\":\"null\"}]})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance containing AnyOfValidator",
          "description": "Ensures that a schema with an \"anyOf\" keyword results in an AnyOfValidator."
        }
      ]
    },
    {
      "group": "Not validator branch",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/not.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"not\":{\"type\":\"null\"}})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance containing NotValidator",
          "description": "Checks that the presence of a \"not\" keyword leads to the creation of a NotValidator."
        }
      ]
    },
    {
      "group": "Conditional schema with only if/then (no else)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/ifThen.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"if\":{\"properties\":{\"age\":{\"minimum\":21}}},\"then\":{\"required\":[\"driverLicense\"]}})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance containing ConditionalValidator (if/then only)",
          "description": "Ensures that a schema with \"if\" and \"then\" but without \"else\" still creates a ConditionalValidator handling the two‑branch case."
        }
      ]
    },
    {
      "group": "Type keyword as an array (multiple allowed types)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/typeArray.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"type\":[\"string\",\"null\"]})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance containing TypeValidator that accepts string or null",
          "description": "Validates that when \"type\" is an array, the factory creates a validator capable of handling multiple allowed types."
        }
      ]
    },
    {
      "group": "Const keyword handling",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/const.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"const\":42})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance containing ConstValidator",
          "description": "Checks that a schema with a \"const\" keyword results in a ConstValidator being attached to the JsonSchema."
        }
      ]
    },
    {
      "group": "Format keyword handling",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/format.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "objectNode({\"type\":\"string\",\"format\":\"date-time\"})" },
            { "parameter": "parentSchema", "value": "null" }
          ],
          "expected": "JsonSchema instance containing FormatValidator for date-time",
          "description": "Ensures that the \"format\" keyword is recognized and results in a FormatValidator appropriate for the specified format."
        }
      ]
    }
  ]
}