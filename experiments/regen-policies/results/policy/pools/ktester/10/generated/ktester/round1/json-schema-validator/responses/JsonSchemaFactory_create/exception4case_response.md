{
  "groups": [
    {
      "group": "JsonSchemaFactory.create – NullNode schemaNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"classpath:/schema.json\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "JsonNodePath.ROOT"
            },
            {
              "parameter": "schemaNode",
              "value": "NullNode.getInstance()"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException) because a NullNode does not represent a valid JSON Schema definition",
          "description": "The factory should reject a schemaNode that is a Jackson NullNode – this triggers the argument‑validation branch that expects an object, array, or boolean schema node."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.create – unknown keyword",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"classpath:/schema.json\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "JsonNodePath.ROOT"
            },
            {
              "parameter": "schemaNode",
              "value": "objectMapper.readTree(\"{ \\\"unknownKeyword\\\": true }\")"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException because the schema contains a keyword that is not recognised by the factory",
          "description": "When the schema node includes a keyword that the factory does not know how to handle, it must fail fast with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.create – $ref with sibling keywords",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"classpath:/schema.json\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "JsonNodePath.ROOT"
            },
            {
              "parameter": "schemaNode",
              "value": "objectMapper.readTree(\"{ \\\"$ref\\\": \\\"#/definitions/foo\\\", \\\"type\\\": \\\"string\\\" }\")"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException because $ref must be the sole keyword in a schema object",
          "description": "The JSON Schema spec forbids any other keywords alongside $ref. The factory should detect this and throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.create – malformed $ref URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"classpath:/schema.json\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "JsonNodePath.ROOT"
            },
            {
              "parameter": "schemaNode",
              "value": "objectMapper.readTree(\"{ \\\"$ref\\\": \\\"http://[invalid]/schema.json\\\" }\")"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException (or JsonSchemaException) caused by an invalid URI in the $ref value",
          "description": "A $ref that cannot be parsed as a URI should cause the factory to abort with an exception rather than attempting resolution."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.create – invalid SchemaLocation URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"ht!tp://[bad]uri\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "JsonNodePath.ROOT"
            },
            {
              "parameter": "schemaNode",
              "value": "objectMapper.readTree(\"{ \\\"type\\\": \\\"string\\\" }\")"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException (or URISyntaxException wrapped) because the supplied schemaLocation string is not a valid URI",
          "description": "SchemaLocation validates its URI during construction; an invalid URI should cause an exception before the factory logic runs."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.create – unknown $schema version",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaLocation",
              "value": "new SchemaLocation(\"classpath:/schema.json\")"
            },
            {
              "parameter": "evaluationPath",
              "value": "JsonNodePath.ROOT"
            },
            {
              "parameter": "schemaNode",
              "value": "objectMapper.readTree(\"{ \\\"$schema\\\": \\\"http://json-schema.org/draft-99/schema#\\\", \\\"type\\\": \\\"number\\\" }\")"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException because the $schema URI does not map to any known draft version",
          "description": "When the $schema keyword points to an unsupported draft, the factory should reject the schema with an IllegalArgumentException."
        }
      ]
    }
  ]
}