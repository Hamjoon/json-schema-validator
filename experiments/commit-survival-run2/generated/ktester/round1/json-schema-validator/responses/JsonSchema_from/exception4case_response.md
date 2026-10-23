{
  "test_cases": [
    {
      "group": "Object Creation - Null SchemaPath",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "null"
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"http://example.com/schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "parent",
              "value": "null"
            },
            {
              "parameter": "suppressSubSchemaRetrieval",
              "value": "false"
            }
          ],
          "expected": "NullPointerException",
          "description": "The factory method should reject a null schemaPath because the JSON‑Pointer location is required for proper schema identification."
        }
      ]
    },
    {
      "group": "Object Creation - Invalid SchemaPath Format",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"properties/name\""
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"http://example.com/schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "parent",
              "value": "null"
            },
            {
              "parameter": "suppressSubSchemaRetrieval",
              "value": "false"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A schemaPath that does not start with a '#' is not a valid JSON‑Pointer; the method must throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Object Creation - Non‑Object SchemaNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#/definitions/list\""
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"http://example.com/schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "JsonNodeFactory.instance.arrayNode()"
            },
            {
              "parameter": "parent",
              "value": "null"
            },
            {
              "parameter": "suppressSubSchemaRetrieval",
              "value": "false"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The schemaNode must be an object node representing a schema definition; passing an array node should cause an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Object Creation - Parent with Different ValidationContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#/properties/child\""
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"http://example.com/child.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "parent",
              "value": "new JsonSchema(new ValidationContext(), \"#\", URI.create(\"http://example.com/parent.json\"), JsonNodeFactory.instance.objectNode(), null, false)"
            },
            {
              "parameter": "suppressSubSchemaRetrieval",
              "value": "false"
            }
          ],
          "expected": "IllegalStateException",
          "description": "If the parent schema was created with a different ValidationContext than the child, the factory should reject the mismatch and throw an IllegalStateException."
        }
      ]
    },
    {
      "group": "Object Creation - Self‑Referential Parent",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#/properties/self\""
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"http://example.com/self.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "parent",
              "value": "JsonSchema.from(new ValidationContext(), \"#\", URI.create(\"http://example.com/self.json\"), JsonNodeFactory.instance.objectNode(), null, false)"
            },
            {
              "parameter": "suppressSubSchemaRetrieval",
              "value": "false"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Providing the same JsonSchema instance as its own parent would create a circular reference; the method should detect this and throw an IllegalArgumentException."
        }
      ]
    }
  ]
}