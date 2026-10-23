{
  "testCases": [
    {
      "group": "Object Creation - Null ValidationContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "null"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#/properties/id\""
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
          "description": "The factory method should reject a null ValidationContext and throw a NullPointerException, because the context is required for validator initialisation."
        }
      ]
    },
    {
      "group": "Object Creation - Null SchemaNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#/properties/age\""
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"http://example.com/person.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "null"
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
          "description": "A null JsonNode representing the schema definition is illegal; the method should throw a NullPointerException."
        }
      ]
    },
    {
      "group": "Object Creation - Relative URI and Schema Path",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#/components/schemas/Address\""
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"http://example.com/api/v1/\")"
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
          "expected": "instance of JsonSchema where getCurrentUri() returns URI.create(\"http://example.com/api/v1/\") and schemaPath equals \"#/components/schemas/Address\"",
          "description": "Verify that a relative base URI is stored unchanged and that the schemaPath is preserved when both are supplied."
        }
      ]
    },
    {
      "group": "Object Creation - Suppress Flag True with Non‑Null Parent",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#/properties/email\""
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"file:/opt/schemas/user.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "parent",
              "value": "new JsonSchema(new ValidationContext(), \"#\", URI.create(\"file:/opt/schemas/root.json\"), JsonNodeFactory.instance.objectNode(), null, false)"
            },
            {
              "parameter": "suppressSubSchemaRetrieval",
              "value": "true"
            }
          ],
          "expected": "instance of JsonSchema with parent != null and suppressSubSchemaRetrieval == true",
          "description": "Ensure that when a non‑null parent schema is provided, the suppress flag can still be set to true and is stored correctly."
        }
      ]
    },
    {
      "group": "Object Creation - Complex SchemaPath with JSON Pointer Escapes",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#/properties/~1special~0name\""
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"http://example.com/complex.json\")"
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
          "expected": "instance of JsonSchema where schemaPath equals \"#/properties/~1special~0name\" (preserving escape sequences)",
          "description": "Validate that the factory method does not decode or alter JSON‑Pointer escape sequences in the schemaPath."
        }
      ]
    },
    {
      "group": "Object Creation - URI with Query and Fragment",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#/definitions/item\""
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"http://example.com/api?version=2#section\")"
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
          "expected": "instance of JsonSchema where getCurrentUri() returns URI.create(\"http://example.com/api?version=2#section\")",
          "description": "Check that a URI containing both a query string and a fragment is stored verbatim."
        }
      ]
    },
    {
      "group": "Object Creation - Parent Chain Multiple Levels",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#/properties/inner\""
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"http://example.com/outer.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "parent",
              "value": "new JsonSchema(new ValidationContext(), \"#/properties/middle\", URI.create(\"http://example.com/middle.json\"), JsonNodeFactory.instance.objectNode(), new JsonSchema(new ValidationContext(), \"#\", URI.create(\"http://example.com/root.json\"), JsonNodeFactory.instance.objectNode(), null, false), false)"
            },
            {
              "parameter": "suppressSubSchemaRetrieval",
              "value": "false"
            }
          ],
          "expected": "instance of JsonSchema where findAncestor() returns the root JsonSchema (the one with schemaPath \"#\")",
          "description": "Create a child schema whose parent itself has a parent, then verify that the newly created schema correctly links to the whole ancestor chain."
        }
      ]
    }
  ]
}